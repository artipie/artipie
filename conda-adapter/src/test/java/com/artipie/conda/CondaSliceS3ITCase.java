/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.conda.http.CondaSlice;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Conda adapter integration test.
 */
public final class CondaSliceS3ITCase {

    /**
     * MinIO S3 storage server port.
     */
    private static final int S3_PORT = 9000;

    /**
     * Curl exit code when resource not retrieved and `--fail` is used, http 400+.
     */
    private static final int CURL_NOT_FOUND = 22;

    /**
     * Exit code template for matching.
     */
    private static final String EXIT_CODE_FMT = "Container.ExecResult(exitCode=%d,";

    /**
     * Don't wait for minIO port on start.
     */
    private static final WaitStrategy DONT_WAIT_PORT = new AbstractWaitStrategy() {
        @Override
        protected void waitUntilReady() {
            // Don't wait for port.
        }
    };

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Application port.
     */
    private int port;

    @BeforeEach
    void initialize() throws Exception {
        this.port = new RandomFreePort().get();
        final Queue<ArtifactEvent> events = new ConcurrentLinkedDeque<>();
        final String url = String.format("http://host.testcontainers.internal:%d", this.port);
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>(
            new ImageFromDockerfile(
                "local/conda-adapter/conda_slice_s3_itcase", false
            ).withDockerfileFromBuilder(
                builder -> builder
                    .from("continuumio/miniconda3:4.10.3")
                    .env("DEBIAN_FRONTEND", "noninteractive")
                    .run("apt update -y -o APT::Update::Error-Mode=any")
                    .run("apt dist-upgrade -y && apt install -y curl netcat")
                    .run("apt autoremove -y && apt clean -y && rm -rfv /var/lib/apt/lists")
                    .run("apt install -y curl && apt clean && rm -rfv /var/lib/apt/lists")
                    .run("conda install -vv -y conda-build==3.27.0 conda-verify==3.4.2 anaconda-client==1.10.0 2>&1|tee /tmp/conda.log")
                    .run("conda clean -a")
                    .run(String.format("echo -e 'channels:\\n  - %s' > /root/.condarc", url))
                    .run(String.format("anaconda config --set url %s/ -s", url))
                    .run("conda config --set anaconda_upload yes")
                    .copy("minio-bin-20231120.txz", "/w/minio-bin-20231120.txz")
                    .run("tar xf /w/minio-bin-20231120.txz -C /root")
                    .run(
                        String.join(
                            ";",
                            "sh -c '/root/bin/minio server /var/minio > /tmp/minio.log 2>&1 &'",
                            "timeout 30 sh -c 'until nc -z localhost 9000; do sleep 0.1; done'",
                            "/root/bin/mc alias set srv1 http://localhost:9000 minioadmin minioadmin 2>&1 |tee /tmp/mc.log",
                            "/root/bin/mc mb srv1/buck1 --region s3test 2>&1|tee -a /tmp/mc.log",
                            "/root/bin/mc anonymous set public srv1/buck1 2>&1|tee -a /tmp/mc.log"
                        )
                    )
                    .run("rm -fv /w/minio-bin-20231120.txz /tmp/*.log")
                    .copy("snappy-1.1.3-0.tar.bz2", "/w/snappy-1.1.3-0.tar.bz2")
                    .copy("noarch_glom-22.1.0.tar.bz2", "/w/noarch_glom-22.1.0.tar.bz2")
                    .copy("linux-64_nng-1.4.0.tar.bz2", "/w/linux-64_nng-1.4.0.tar.bz2")
                )
                .withFileFromClasspath("snappy-1.1.3-0.tar.bz2", "snappy-1.1.3-0.tar.bz2")
                .withFileFromClasspath("noarch_glom-22.1.0.tar.bz2", "noarch_glom-22.1.0.tar.bz2")
                .withFileFromClasspath("linux-64_nng-1.4.0.tar.bz2", "linux-64_nng-1.4.0.tar.bz2")
                .withFileFromClasspath("minio-bin-20231120.txz", "minio-bin-20231120.txz")
        )
        .withExposedPorts(CondaSliceS3ITCase.S3_PORT)
        .waitingFor(CondaSliceS3ITCase.DONT_WAIT_PORT)
        .withCommand("tail", "-f", "/dev/null")
        .withWorkingDirectory("/home/");
        this.cntn.start();
        final int minioport = this.cntn.getMappedPort(9000);
        final Storage storage = new StoragesLoader()
            .newObject(
                "s3",
                new Config.YamlStorageConfig(
                    Yaml.createYamlMappingBuilder()
                        .add("region", "s3test")
                        .add("bucket", "buck1")
                        .add("endpoint", String.format("http://localhost:%d", minioport))
                        .add(
                            "credentials",
                            Yaml.createYamlMappingBuilder()
                                .add("type", "basic")
                                .add("accessKeyId", "minioadmin")
                                .add("secretAccessKey", "minioadmin")
                                .build()
                        )
                        .build()
                )
            );
        this.server = new VertxSliceServer(
            CondaSliceS3ITCase.VERTX,
            new LoggingSlice(
                new BodyLoggingSlice(
                    new CondaSlice(
                        storage, Policy.FREE,
                        (username, password) -> Optional.of(AuthUser.ANONYMOUS),
                        new TestCondaTokens(), url, "*", Optional.of(events)
                    )
                )
            ),
            this.port
        );
        this.server.start();
        MatcherAssert.assertThat(
            "Failed to start Minio",
            this.exec("bash", "-c", "nohup /root/bin/minio server /var/minio 2>&1|tee /tmp/minio.log &"),
            this.exitCodeStrMatcher(0)
        );
        MatcherAssert.assertThat(
            "Failed to wait for Minio",
            this.exec("timeout", "30",  "sh", "-c", "until nc -z localhost 9000; do sleep 0.1; done"),
            this.exitCodeStrMatcher(0)
        );
        MatcherAssert.assertThat(
            "Login was not successful",
            this.exec("anaconda login --username any --password any".split(" ")),
            this.exitCodeStrMatcher(0)
        );
    }

    @AfterEach
    void stop() {
        this.server.stop();
        this.cntn.stop();
    }

    @ParameterizedTest
    @CsvSource({
        "noarch_glom-22.1.0.tar.bz2,glom/22.1.0/noarch,noarch",
        "snappy-1.1.3-0.tar.bz2,snappy/1.1.3/linux-64,linux-64"
    })
    void canSingleUploadToArtipie(final String pkgname, final String pkgpath, final String pkgarch)
        throws Exception {
        MatcherAssert.assertThat(
            "repodata.json must be absent in S3 before test",
            this.exec(String.format("curl -f -kv http://localhost:9000/buck1/%s/repodata.json", pkgarch).split(" ")),
            this.exitCodeStrMatcher(CondaSliceS3ITCase.CURL_NOT_FOUND)
        );
        MatcherAssert.assertThat(
            String.format("%s must be absent in S3 before test", pkgname),
            this.exec(String.format("curl -f -kv http://localhost:9000/buck1/%s/%s", pkgarch, pkgname).split(" ")),
            this.exitCodeStrMatcher(CondaSliceS3ITCase.CURL_NOT_FOUND)
        );
        MatcherAssert.assertThat(
            "Package was not uploaded successfully",
            this.exec(String.format("timeout 30s anaconda --show-traceback --verbose upload /w/%s", pkgname).split(" ")),
            new StringContainsInOrder(
                new ListOf<>(
                    String.format(CondaSliceS3ITCase.EXIT_CODE_FMT, 0),
                    String.format("Using Anaconda API: http://host.testcontainers.internal:%d/", this.port),
                    String.format("Uploading file \"anonymous/%s/%s\"", pkgpath, pkgname),
                    "Upload complete"
                )
            )
        );
        MatcherAssert.assertThat(
            "repodata.json must exist in S3 storage after test",
            this.exec(String.format("curl -f -kv http://localhost:9000/buck1/%s/repodata.json", pkgarch).split(" ")),
            this.exitCodeStrMatcher(0)
        );
        MatcherAssert.assertThat(
            String.format("%s/%s must exist in S3 storage after test", pkgarch, pkgname),
            this.exec(String.format("curl -f -kv http://localhost:9000/buck1/%s/%s", pkgarch, pkgname).split(" ")),
            this.exitCodeStrMatcher(0)
        );
    }

    @Test
    void canMultiUploadDifferentArchTest() throws Exception {
        MatcherAssert.assertThat(
            "linux-64/snappy-1.1.3-0.tar.bz2 must be absent in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/linux-64/snappy-1.1.3-0.tar.bz2".split(" ")),
            this.exitCodeStrMatcher(CondaSliceS3ITCase.CURL_NOT_FOUND)
        );
        MatcherAssert.assertThat(
            "noarch_glom-22.1.0.tar.bz2 must be absent in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/noarch/noarch_glom-22.1.0.tar.bz2".split(" ")),
            this.exitCodeStrMatcher(CondaSliceS3ITCase.CURL_NOT_FOUND)
        );
        MatcherAssert.assertThat(
            "noarch/repodata.json must be absent in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/noarch/repodata.json".split(" ")),
            this.exitCodeStrMatcher(CondaSliceS3ITCase.CURL_NOT_FOUND)
        );
        MatcherAssert.assertThat(
            "linux-64/repodata.json must be absent in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/linux-64/repodata.json".split(" ")),
            this.exitCodeStrMatcher(CondaSliceS3ITCase.CURL_NOT_FOUND)
        );
        MatcherAssert.assertThat(
            "Package was not uploaded successfully",
            this.exec("timeout 30s anaconda --show-traceback --verbose upload /w/snappy-1.1.3-0.tar.bz2".split(" ")),
            new StringContainsInOrder(
                new ListOf<>(
                    String.format(CondaSliceS3ITCase.EXIT_CODE_FMT, 0),
                    String.format("Using Anaconda API: http://host.testcontainers.internal:%d/", this.port),
                    "Uploading file \"anonymous/snappy/1.1.3/linux-64/snappy-1.1.3-0.tar.bz2\"",
                    "Upload complete"
                )
            )
        );
        MatcherAssert.assertThat(
            "Package was not uploaded successfully",
            this.exec("timeout 30s anaconda --show-traceback --verbose upload /w/noarch_glom-22.1.0.tar.bz2".split(" ")),
            new StringContainsInOrder(
                new ListOf<>(
                    String.format(CondaSliceS3ITCase.EXIT_CODE_FMT, 0),
                    String.format("Using Anaconda API: http://host.testcontainers.internal:%d/", this.port),
                    "Uploading file \"anonymous/glom/22.1.0/noarch/noarch_glom-22.1.0.tar.bz2\"",
                    "Upload complete"
                )
            )
        );
        MatcherAssert.assertThat(
            "linux-64/snappy-1.1.3-0.tar.bz2 must exist in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/linux-64/snappy-1.1.3-0.tar.bz2".split(" ")),
            this.exitCodeStrMatcher(0)
        );
        MatcherAssert.assertThat(
            "noarch_glom-22.1.0.tar.bz2 must exist in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/noarch/noarch_glom-22.1.0.tar.bz2".split(" ")),
            this.exitCodeStrMatcher(0)
        );
        MatcherAssert.assertThat(
            "noarch/repodata.json must exist in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/noarch/repodata.json".split(" ")),
            this.exitCodeStrMatcher(0)
        );
        MatcherAssert.assertThat(
            "linux-64/repodata.json must exist in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/linux-64/repodata.json".split(" ")),
            this.exitCodeStrMatcher(0)
        );
    }

    @Test
    void canMultiUploadSameArchTest() throws Exception {
        MatcherAssert.assertThat(
            "linux-64/snappy-1.1.3-0.tar.bz2 must be absent in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/linux-64/snappy-1.1.3-0.tar.bz2".split(" ")),
            this.exitCodeStrMatcher(CondaSliceS3ITCase.CURL_NOT_FOUND)
        );
        MatcherAssert.assertThat(
            "linux-64/linux-64_nng-1.4.0.tar.bz2 must be absent in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/linux-64/linux-64_nng-1.4.0.tar.bz2".split(" ")),
            this.exitCodeStrMatcher(CondaSliceS3ITCase.CURL_NOT_FOUND)
        );
        MatcherAssert.assertThat(
            "linux-64/linux-64_nng-1.4.0.tar.bz2 must be absent in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/linux-64/repodata.json".split(" ")),
            this.exitCodeStrMatcher(CondaSliceS3ITCase.CURL_NOT_FOUND)
        );
        MatcherAssert.assertThat(
            "Package was not uploaded successfully",
            this.exec("timeout 30s anaconda --show-traceback --verbose upload /w/linux-64_nng-1.4.0.tar.bz2".split(" ")),
            new StringContainsInOrder(
                new ListOf<>(
                    String.format(CondaSliceS3ITCase.EXIT_CODE_FMT, 0),
                    String.format("Using Anaconda API: http://host.testcontainers.internal:%d/", this.port),
                    "Uploading file \"anonymous/nng/1.4.0/linux-64/linux-64_nng-1.4.0.tar.bz2\"",
                    "Upload complete"
                )
            )
        );
        MatcherAssert.assertThat(
            "Package was not uploaded successfully",
            this.exec("timeout 30s anaconda --show-traceback --verbose upload /w/snappy-1.1.3-0.tar.bz2".split(" ")),
            new StringContainsInOrder(
                new ListOf<>(
                    String.format(CondaSliceS3ITCase.EXIT_CODE_FMT, 0),
                    String.format("Using Anaconda API: http://host.testcontainers.internal:%d/", this.port),
                    "Uploading file \"anonymous/snappy/1.1.3/linux-64/snappy-1.1.3-0.tar.bz2\"",
                    "Upload complete"
                )
            )
        );
        MatcherAssert.assertThat(
            "linux-64/snappy-1.1.3-0.tar.bz2 must exist in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/linux-64/snappy-1.1.3-0.tar.bz2".split(" ")),
            this.exitCodeStrMatcher(0)
        );
        MatcherAssert.assertThat(
            "linux-64/linux-64_nng-1.4.0.tar.bz2 must exist in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/linux-64/linux-64_nng-1.4.0.tar.bz2".split(" ")),
            this.exitCodeStrMatcher(0)
        );
        MatcherAssert.assertThat(
            "linux-64/linux-64_nng-1.4.0.tar.bz2 must exist in S3 before test",
            this.exec("curl -f -kv http://localhost:9000/buck1/linux-64/repodata.json".split(" ")),
            this.exitCodeStrMatcher(0)
        );
    }

    private String exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res.toString();
    }

    private StringContainsInOrder exitCodeStrMatcher(final int code) {
        return new StringContainsInOrder(
            new ListOf<>(String.format(CondaSliceS3ITCase.EXIT_CODE_FMT, code))
        );
    }
}
