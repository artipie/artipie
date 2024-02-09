/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import org.cactoos.list.ListOf;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import java.io.IOException;

/**
 * Integration tests for Pypi repository.
 *
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class PypiS3ITCase {

    /**
     * Curl exit code when resource not retrieved and `--fail` is used, http 400+.
     */
    private static final int CURL_NOT_FOUND = 22;

    /**
     * Test deployments.
     *
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("pypi-repo/pypi-s3.yml", "my-python")
            .withUser("security/users/alice.yaml", "alice")
            .withRole("security/roles/readers.yaml", "readers")
            .withExposedPorts(8080),
        () -> new TestDeployment.ClientContainer(
            new ImageFromDockerfile(
                "local/artipie-main/pypi_s3_itcase", false
            ).withDockerfileFromBuilder(
                builder -> builder
                    .from("python:3.7")
                    .run("apt update -y -o APT::Update::Error-Mode=any")
                    .run("apt dist-upgrade -y && apt install -y curl xz-utils netcat-traditional")
                    .run("apt autoremove -y && apt clean -y && rm -rfv /var/lib/apt/lists")
                    .run("pip install -U pip setuptools")
                    .run("pip install -U twine")
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
            ).withFileFromClasspath("minio-bin-20231120.txz", "minio-bin-20231120.txz")
        )
            .withWorkingDirectory("/w")
            .withNetworkAliases("minioc")
            .withExposedPorts(9000)
            .withClasspathResourceMapping(
                "pypi-repo/example-pckg", "/w/example-pckg", BindMode.READ_ONLY
            )
            .waitingFor(
                new AbstractWaitStrategy() {
                    @Override
                    protected void waitUntilReady() {
                        // Don't wait for minIO port.
                    }
                }
            )
    );

    @BeforeEach
    void setUp() throws IOException {
        this.containers.assertExec(
            "Failed to start Minio", new ContainerResultMatcher(),
            "bash", "-c", "nohup /root/bin/minio server /var/minio 2>&1|tee /tmp/minio.log &"
        );
        this.containers.assertExec(
            "Failed to wait for Minio", new ContainerResultMatcher(),
            "timeout", "30",  "sh", "-c", "until nc -z localhost 9000; do sleep 0.1; done"
        );
    }

    @ParameterizedTest
    @CsvSource("8080,my-python,9000")
    //"8081,my-python-port,9000" todo https://github.com/artipie/artipie/issues/1350
    void uploadAndinstallPythonPackage(final String port, final String repo, final String s3port) throws IOException {
        this.containers.assertExec(
            "artipietestpkg-0.0.3.tar.gz must not exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(PypiS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/my-python/artipietestpkg/artipietestpkg-0.0.3.tar.gz".formatted(s3port, repo).split(" ")
        );
        this.containers.assertExec(
            "Failed to upload",
            new ContainerResultMatcher(
                Matchers.is(0),
                new StringContainsInOrder(
                    new ListOf<>(
                        "Uploading artipietestpkg-0.0.3.tar.gz", "100%"
                    )
                )
            ),
            "python3", "-m", "twine", "upload", "--repository-url",
            String.format("http://artipie:%s/%s/", port, repo),
            "-u", "alice", "-p", "123",
            "/w/example-pckg/dist/artipietestpkg-0.0.3.tar.gz"
        );
        this.containers.assertExec(
            "Failed to install package",
            new ContainerResultMatcher(
                Matchers.equalTo(0),
                new StringContainsInOrder(
                    new ListOf<>(
                        String.format("Looking in indexes: http://artipie:%s/%s", port, repo),
                        "Collecting artipietestpkg",
                        String.format(
                            "  Downloading http://artipie:%s/%s/artipietestpkg/%s",
                            port, repo, "artipietestpkg-0.0.3.tar.gz"
                        ),
                        "Building wheels for collected packages: artipietestpkg",
                        "  Building wheel for artipietestpkg (setup.py): started",
                        String.format(
                            "  Building wheel for artipietestpkg (setup.py): %s",
                            "finished with status 'done'"
                        ),
                        "Successfully built artipietestpkg",
                        "Installing collected packages: artipietestpkg",
                        "Successfully installed artipietestpkg-0.0.3"
                    )
                )
            ),
            "python", "-m", "pip", "install", "--trusted-host", "artipie", "--index-url",
            String.format("http://artipie:%s/%s", port, repo),
            "artipietestpkg"
        );
        this.containers.assertExec(
            "artipietestpkg-0.0.3.tar.gz must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minioc:%s/buck1/my-python/artipietestpkg/artipietestpkg-0.0.3.tar.gz".formatted(s3port, repo).split(" ")
        );
    }

    @ParameterizedTest
    @CsvSource("8080,my-python,9000")
    //"8081,my-python-port,9000" todo https://github.com/artipie/artipie/issues/1350
    void canUpload(final String port, final String repo, final String s3port) throws Exception {
        this.containers.assertExec(
            "artipietestpkg-0.0.3.tar.gz must not exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(PypiS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/my-python/artipietestpkg/artipietestpkg-0.0.3.tar.gz".formatted(s3port, repo).split(" ")
        );
        this.containers.assertExec(
            "Failed to upload",
            new ContainerResultMatcher(
                Matchers.is(0),
                new StringContainsInOrder(
                    new ListOf<>(
                        "Uploading artipietestpkg-0.0.3.tar.gz", "100%"
                    )
                )
            ),
            "python3", "-m", "twine", "upload", "--repository-url",
            String.format("http://artipie:%s/%s/", port, repo),
            "-u", "alice", "-p", "123",
            "/w/example-pckg/dist/artipietestpkg-0.0.3.tar.gz"
        );
        this.containers.assertExec(
            "artipietestpkg-0.0.3.tar.gz must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minioc:%s/buck1/my-python/artipietestpkg/artipietestpkg-0.0.3.tar.gz".formatted(s3port, repo).split(" ")
        );
    }
}
