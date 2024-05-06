/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Conda adapter integration test.
 */
public final class CondaSliceS3ITCase {

    @RegisterExtension
    static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * S3 storage server port.
     */
    private static final int S3_PORT = 9000;

    /**
     * Exit code template for matching.
     */
    private static final String EXIT_CODE_FMT = "Container.ExecResult(exitCode=%d,";

    /**
     * Don't wait for S3 port on start.
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
     * Bucket to use in tests.
     */
    private String bucket;

    /**
     * Artipie Storage instance for tests.
     */
    private Storage storage;

    /**
     * Application port.
     */
    private int port;

    @BeforeEach
    void initialize(final AmazonS3 client) throws Exception {
        this.bucket = UUID.randomUUID().toString();
        client.createBucket(this.bucket);
        this.storage = StoragesLoader.STORAGES
            .newObject(
                "s3",
                new Config.YamlStorageConfig(
                    Yaml.createYamlMappingBuilder()
                        .add("region", "us-east-1")
                        .add("bucket", this.bucket)
                        .add("endpoint", String.format("http://localhost:%d", MOCK.getHttpPort()))
                        .add(
                            "credentials",
                            Yaml.createYamlMappingBuilder()
                                .add("type", "basic")
                                .add("accessKeyId", "foo")
                                .add("secretAccessKey", "bar")
                                .build()
                        )
                        .build()
                )
            );
        this.port = RandomFreePort.get();
        final Queue<ArtifactEvent> events = new ConcurrentLinkedDeque<>();
        final String url = String.format("http://host.testcontainers.internal:%d", this.port);
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("artipie/conda-tests:1.0")
        .withExposedPorts(CondaSliceS3ITCase.S3_PORT)
        .waitingFor(CondaSliceS3ITCase.DONT_WAIT_PORT)
        .withCommand("tail", "-f", "/dev/null")
        .withWorkingDirectory("/w/adapter/example-project");
        this.cntn.start();
        this.server = new VertxSliceServer(
            CondaSliceS3ITCase.VERTX,
            new LoggingSlice(
                new BodyLoggingSlice(
                    new CondaSlice(
                        this.storage, Policy.FREE,
                        (username, password) -> Optional.of(AuthUser.ANONYMOUS),
                        new TestCondaTokens(), url, "*", Optional.of(events)
                    )
                )
            ),
            this.port
        );
        this.server.start();
        MatcherAssert.assertThat(
            "Failed to update /root/.condarc",
            this.exec("sh", "-c", String.format("echo -e 'channels:\\n  - %s' > /root/.condarc", url)),
            this.exitCodeStrMatcher(0)
        );
        MatcherAssert.assertThat(
            "Failed to set anaconda url",
            this.exec(String.format("anaconda config --set url %s/ -s", url).split(" ")),
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
        final Key pkgKey = new Key.From("%s/%s".formatted(pkgarch, pkgname));
        final Key repodata = new Key.From("%s/repodata.json".formatted(pkgarch));
        MatcherAssert.assertThat(
            String.format("%s must be absent in S3 before test", pkgname),
            !this.storage.exists(pkgKey).get()
        );
        MatcherAssert.assertThat(
            "repodata.json must be absent in S3 before test",
            !this.storage.exists(repodata).get()
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
            String.format("%s must exist in S3 after test", pkgname),
            this.storage.exists(pkgKey).get()
        );
        MatcherAssert.assertThat(
            "repodata.json must exist in S3 after test",
            this.storage.exists(repodata).get()
        );
    }

    @Test
    void canMultiUploadDifferentArchTest() throws Exception {
        MatcherAssert.assertThat(
            "linux-64/snappy-1.1.3-0.tar.bz2 must be absent in S3 before test",
            !this.storage.exists(new Key.From("linux-64/snappy-1.1.3-0.tar.bz2")).get()
        );
        MatcherAssert.assertThat(
            "noarch_glom-22.1.0.tar.bz2 must be absent in S3 before test",
            !this.storage.exists(new Key.From("noarch/noarch_glom-22.1.0.tar.bz2")).get()
        );
        MatcherAssert.assertThat(
            "noarch/repodata.json must be absent in S3 before test",
            !this.storage.exists(new Key.From("noarch/repodata.json")).get()
        );
        MatcherAssert.assertThat(
            "linux-64/repodata.json must be absent in S3 before test",
            !this.storage.exists(new Key.From("linux-64/repodata.json")).get()
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
            this.storage.exists(new Key.From("linux-64/snappy-1.1.3-0.tar.bz2")).get()
        );
        MatcherAssert.assertThat(
            "noarch_glom-22.1.0.tar.bz2 must exist in S3 before test",
            this.storage.exists(new Key.From("noarch/noarch_glom-22.1.0.tar.bz2")).get()
        );
        MatcherAssert.assertThat(
            "noarch/repodata.json must exist in S3 before test",
            this.storage.exists(new Key.From("noarch/repodata.json")).get()
        );
        MatcherAssert.assertThat(
            "linux-64/repodata.json must exist in S3 before test",
            this.storage.exists(new Key.From("linux-64/repodata.json")).get()
        );
    }

    @Test
    void canMultiUploadSameArchTest() throws Exception {
        MatcherAssert.assertThat(
            "linux-64/snappy-1.1.3-0.tar.bz2 must be absent in S3 before test",
            !this.storage.exists(new Key.From("linux-64/snappy-1.1.3-0.tar.bz2")).get()
        );
        MatcherAssert.assertThat(
            "linux-64/linux-64_nng-1.4.0.tar.bz2 must be absent in S3 before test",
            !this.storage.exists(new Key.From("linux-64/linux-64_nng-1.4.0.tar.bz2")).get()
        );
        MatcherAssert.assertThat(
            "linux-64/repodata.json must be absent in S3 before test",
            !this.storage.exists(new Key.From("linux-64/repodata.json")).get()
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
            this.storage.exists(new Key.From("linux-64/snappy-1.1.3-0.tar.bz2")).get()
        );
        MatcherAssert.assertThat(
            "linux-64/linux-64_nng-1.4.0.tar.bz2 must exist in S3 before test",
            this.storage.exists(new Key.From("linux-64/linux-64_nng-1.4.0.tar.bz2")).get()
        );
        MatcherAssert.assertThat(
            "linux-64/repodata.json must exist in S3 before test",
            this.storage.exists(new Key.From("linux-64/repodata.json")).get()
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
