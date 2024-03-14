/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conan;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.util.Arrays;

/**
 * Integration tests for Conan repository.
 * @since 0.23
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class ConanS3ITCase {

    /**
     * MinIO S3 storage server port.
     */
    private static final int S3_PORT = 9000;

    /**
     * Test repository name.
     */
    private static final String REPO = "my-conan";

    /**
     * Path prefix to conan repository test data in java resources.
     */
    private static final String SRV_RES_PREFIX = "conan/conan_server/data";

    /**
     * Client path for conan package binary data file.
     */
    private static final String CLIENT_BIN_PKG = "/root/.conan/data/zlib/1.2.13/_/_/dl/pkg/dfbe50feef7f3c6223a476cd5aeadb687084a646/conan_package.tgz";

    /**
     * Conan server subpath for conan package binary data file.
     */
    private static final String SERVER_BIN_PKG = "zlib/1.2.13/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/0/conan_package.tgz";

    /**
     * conan server repository path for conan package binary data file.
     */
    private static final Key REPO_BIN_PKG = new Key.From(new Key.From(ConanS3ITCase.REPO), SERVER_BIN_PKG.split(Key.DELIMITER));

    /**
     * S3Storage of Artipie repository.
     */
    private Storage repository;

    /**
     * Conan client test container.
     */
    private TestDeployment.ClientContainer client;

    /**
     * Test deployments.
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withUser("security/users/alice.yaml", "alice")
            .withRepoConfig("conan/conan-s3.yml", ConanS3ITCase.REPO)
            .withExposedPorts(9301),
        () -> {
            this.client = prepareClientContainer();
            return this.client;
        }
    );

    @BeforeEach
    void init() throws IOException {
        this.containers.assertExec(
            "Failed to start Minio", new ContainerResultMatcher(),
            "bash", "-c", "nohup /root/bin/minio server /var/minio 2>&1|tee /tmp/minio.log &"
        );
        this.containers.assertExec(
            "Failed to wait for Minio", new ContainerResultMatcher(),
            "timeout", "30",  "sh", "-c", "until nc -z localhost 9000; do sleep 0.1; done"
        );
        final int s3port = this.client.getMappedPort(ConanS3ITCase.S3_PORT);
        this.repository = new StoragesLoader()
            .newObject(
                "s3",
                new Config.YamlStorageConfig(
                    Yaml.createYamlMappingBuilder()
                        .add("region", "s3test")
                        .add("bucket", "buck1")
                        .add("endpoint", String.format("http://localhost:%d", s3port))
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
    }

    @Test
    public void incorrectPortFailTest() throws IOException {
        new TestResource(ConanS3ITCase.SRV_RES_PREFIX).addFilesTo(this.repository, new Key.From(ConanS3ITCase.REPO));
        this.containers.assertExec(
            "Conan remote add failed", new ContainerResultMatcher(),
            "conan remote add -f conan-test http://artipie:9300 False".split(" ")
        );
        this.containers.assertExec(
            "Conan remote add failed", new ContainerResultMatcher(
                new IsNot<>(new IsEqual<>(ContainerResultMatcher.SUCCESS))
            ),
            "conan install zlib/1.2.13@ -r conan-test -b -pr:b=default".split(" ")
        );
    }

    @Test
    public void incorrectPkgFailTest() throws IOException {
        new TestResource(ConanS3ITCase.SRV_RES_PREFIX).addFilesTo(this.repository, new Key.From(ConanS3ITCase.REPO));
        this.containers.assertExec(
            "Conan remote add failed", new ContainerResultMatcher(
                new IsNot<>(new IsEqual<>(ContainerResultMatcher.SUCCESS))
            ),
            "conan install zlib/1.2.11@ -r conan-test -b -pr:b=default".split(" ")
        );
    }

    @Test
    public void installFromArtipie() throws IOException, InterruptedException {
        MatcherAssert.assertThat(
            "Binary package must not exist in cache before test",
            this.client.execInContainer("ls", CLIENT_BIN_PKG).getExitCode() > 0
        );
        MatcherAssert.assertThat(
            "Server key must not exist before copying",
            !this.repository.exists(ConanS3ITCase.REPO_BIN_PKG).join()
        );
        new TestResource(ConanS3ITCase.SRV_RES_PREFIX)
            .addFilesTo(this.repository, new Key.From(ConanS3ITCase.REPO));
        MatcherAssert.assertThat(
            "Server key must exist after copying",
            this.repository.exists(ConanS3ITCase.REPO_BIN_PKG).join()
        );
        this.containers.assertExec(
            "Conan install failed", new ContainerResultMatcher(),
            "conan install zlib/1.2.13@ -r conan-test".split(" ")
        );
        final byte[] original = new TestResource(
            String.join("/", ConanS3ITCase.SRV_RES_PREFIX, SERVER_BIN_PKG)
        ).asBytes();
        final byte[] downloaded = this.client.copyFileFromContainer(
            CLIENT_BIN_PKG, IOUtils::toByteArray
        );
        MatcherAssert.assertThat("Files content must match", Arrays.equals(original, downloaded));
    }

    @Test
    public void uploadToArtipie() throws IOException {
        MatcherAssert.assertThat(
            "Server key must not exist before test",
            !this.repository.exists(ConanS3ITCase.REPO_BIN_PKG).join()
        );
        this.containers.assertExec(
            "Conan install failed", new ContainerResultMatcher(),
            "conan install zlib/1.2.13@ -r conancenter".split(" ")
        );
        this.containers.assertExec(
            "Conan upload failed", new ContainerResultMatcher(),
            "conan upload zlib/1.2.13@ -r conan-test --all".split(" ")
        );
        MatcherAssert.assertThat(
            "Server key must exist after test",
            this.repository.exists(ConanS3ITCase.REPO_BIN_PKG).join()
        );
    }

    @Test
    public void uploadFailtest() throws IOException {
        MatcherAssert.assertThat(
            "Server key must not exist before test",
            !this.repository.exists(ConanS3ITCase.REPO_BIN_PKG).join()
        );
        this.containers.assertExec(
            "Conan upload failed", new ContainerResultMatcher(
                new IsNot<>(new IsEqual<>(ContainerResultMatcher.SUCCESS))
            ),
            "conan upload zlib/1.2.13@ -r conan-test --all".split(" ")
        );
        MatcherAssert.assertThat(
            "Server key must not exist after test",
            !this.repository.exists(ConanS3ITCase.REPO_BIN_PKG).join()
        );
    }

    @Test
    void testPackageReupload() throws IOException {
        MatcherAssert.assertThat(
            "Server key must not exist before test",
            !this.repository.exists(ConanS3ITCase.REPO_BIN_PKG).join()
        );
        this.containers.assertExec(
            "Conan install (conancenter) failed", new ContainerResultMatcher(),
            "conan install zlib/1.2.13@ -r conancenter".split(" ")
        );
        this.containers.assertExec(
            "Conan upload failed", new ContainerResultMatcher(),
            "conan upload zlib/1.2.13@ -r conan-test --all".split(" ")
        );
        MatcherAssert.assertThat(
            "Server key must exist after upload",
            this.repository.exists(ConanS3ITCase.REPO_BIN_PKG).join()
        );
        this.containers.assertExec(
            "rm cache failed", new ContainerResultMatcher(),
            "rm -rf /root/.conan/data".split(" ")
        );
        this.containers.assertExec(
            "Conan install (conan-test) failed", new ContainerResultMatcher(),
            "conan install zlib/1.2.13@ -r conan-test".split(" ")
        );
        MatcherAssert.assertThat(
            "Server key must exist after test",
            this.repository.exists(ConanS3ITCase.REPO_BIN_PKG).join()
        );
    }

    /**
     * Prepares base docker image instance for tests.
     *
     * @return ImageFromDockerfile of testcontainers.
     */
    @SuppressWarnings("PMD.LineLengthCheck")
    private static TestDeployment.ClientContainer prepareClientContainer() {
        final ImageFromDockerfile image = new ImageFromDockerfile(
            "local/artipie-main/conan_s3_itcase", false
        ).withDockerfileFromBuilder(
            builder -> builder
                .from("ubuntu:22.04")
                .env("CONAN_TRACE_FILE", "/tmp/conan_trace.log")
                .env("DEBIAN_FRONTEND", "noninteractive")
                .env("CONAN_VERBOSE_TRACEBACK", "1")
                .env("CONAN_NON_INTERACTIVE", "1")
                .env("no_proxy", "host.docker.internal,host.testcontainers.internal,localhost,127.0.0.1")
                .workDir("/home")
                .run("apt clean -y && apt update -y -o APT::Update::Error-Mode=any")
                .run("apt install --no-install-recommends -y python3-pip curl g++ git make cmake xz-utils netcat")
                .run("pip3 install -U pip setuptools")
                .run("pip3 install -U conan==1.60.2")
                .run("conan profile new --detect default")
                .run("conan profile update settings.compiler.libcxx=libstdc++11 default")
                .run("conan remote add conancenter https://center.conan.io False --force")
                .run("conan remote add conan-center https://conan.bintray.com False --force")
                .run("conan remote add conan-test http://artipie:9301 False --force")
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
        ).withFileFromClasspath("minio-bin-20231120.txz", "minio-bin-20231120.txz");
        return new TestDeployment.ClientContainer(image)
            .withCommand("tail", "-f", "/dev/null")
            .withAccessToHost(true)
            .withWorkingDirectory("/w")
            .withNetworkAliases("minic")
            .withExposedPorts(ConanS3ITCase.S3_PORT)
            .waitingFor(
                new AbstractWaitStrategy() {
                    @Override
                    protected void waitUntilReady() {
                        // Don't wait for minIO S3 port.
                    }
                }
            );
    }
}
