/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.cactoos.list.ListOf;
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

/**
 * Debian integration test.
 * @since 0.15
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class DebianS3ITCase {

    /**
     * Curl exit code when resource not retrieved and `--fail` is used, http 400+.
     */
    private static final int CURL_NOT_FOUND = 22;

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("debian/debian-s3.yml", "my-debian")
            .withExposedPorts(8080),
        () -> new TestDeployment.ClientContainer(
            new ImageFromDockerfile(
                "local/artipie-main/debian_s3_itcase", false
            ).withDockerfileFromBuilder(
                builder -> builder
                    .from("debian:10.8-slim")
                    .env("DEBIAN_FRONTEND", "noninteractive")
                    .run("apt update -y -o APT::Update::Error-Mode=any")
                    .run("apt dist-upgrade -y && apt install -y curl xz-utils netcat")
                    .run("apt autoremove -y && apt clean -y && rm -rfv /var/lib/apt/lists")
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
            "debian/aglfn_1.7-3_amd64.deb", "/w/aglfn_1.7-3_amd64.deb", BindMode.READ_ONLY
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
    @CsvSource({
        "8080,my-debian,9000"
    })
    void curlPutWorks(final String port, final String repo, final String s3port) throws Exception {
        this.containers.assertExec(
            "Packages.gz must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(DebianS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/%s/dists/my-debian/main/binary-amd64/Packages.gz".formatted(s3port, repo).split(" ")
        );
        this.containers.assertExec(
            "aglfn_1.7-3_amd64.deb must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(DebianS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/%s/main/aglfn_1.7-3_amd64.deb".formatted(s3port, repo).split(" ")
        );
        this.containers.assertExec(
            "Failed to upload deb package",
            new ContainerResultMatcher(),
            "timeout", "30s", "curl", "-i", "-X", "PUT", "--data-binary", "@/w/aglfn_1.7-3_amd64.deb", String.format("http://artipie:%s/%s/main/aglfn_1.7-3_amd64.deb", port, repo)
        );
        this.containers.assertExec(
            "aglfn_1.7-3_amd64.deb must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minioc:%s/buck1/%s/main/aglfn_1.7-3_amd64.deb".formatted(s3port, repo).split(" ")
        );
        this.containers.assertExec(
            "Packages.gz must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minioc:%s/buck1/%s/dists/my-debian/main/binary-amd64/Packages.gz".formatted(s3port, repo).split(" ")
        );
        this.containers.assertExec(
            "deb from repo must be downloadable",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "timeout 30s curl -f -k http://artipie:%s/%s/main/aglfn_1.7-3_amd64.deb -o /home/aglfn_repo.deb".formatted(port, repo).split(" ")
        );
        this.containers.assertExec(
            "deb from repo must match with original",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "cmp /w/aglfn_1.7-3_amd64.deb /home/aglfn_repo.deb".formatted(s3port, repo).split(" ")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "8080,my-debian,9000"
    })
    void pushAndInstallWorks(final String port, final String repo, final String s3port) throws Exception {
        this.containers.assertExec(
            "Packages.gz must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(DebianS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/%s/dists/my-debian/main/binary-amd64/Packages.gz".formatted(s3port, repo).split(" ")
        );
        this.containers.assertExec(
            "aglfn_1.7-3_amd64.deb must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(DebianS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/%s/main/aglfn_1.7-3_amd64.deb".formatted(s3port, repo).split(" ")
        );
        this.containers.putBinaryToClient(
            String.format(
                "deb [trusted=yes] http://artipie:%s/%s %s main", port, repo, repo
            ).getBytes(),
            "/etc/apt/sources.list"
        );
        this.containers.assertExec(
            "Failed to upload deb package",
            new ContainerResultMatcher(),
            "timeout", "30s", "curl", String.format("http://artipie:%s/%s/main/aglfn_1.7-3_amd64.deb", port, repo),
            "--upload-file", "/w/aglfn_1.7-3_amd64.deb"
        );
        this.containers.assertExec(
            "Apt-get update failed",
            new ContainerResultMatcher(),
            "apt-get", "update", "-y"
        );
        this.containers.assertExec(
            "Package was not downloaded and unpacked",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(new ListOf<>("Unpacking aglfn", "Setting up aglfn"))
            ),
            "apt-get", "install", "-y", "aglfn"
        );
        this.containers.assertExec(
            "aglfn_1.7-3_amd64.deb must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minioc:%s/buck1/%s/main/aglfn_1.7-3_amd64.deb".formatted(s3port, repo).split(" ")
        );
        this.containers.assertExec(
            "Packages.gz must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minioc:%s/buck1/%s/dists/my-debian/main/binary-amd64/Packages.gz".formatted(s3port, repo).split(" ")
        );
    }
}
