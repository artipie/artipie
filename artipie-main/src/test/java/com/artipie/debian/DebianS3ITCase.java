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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

/**
 * Debian integration test.
 * @since 0.15
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@Execution(ExecutionMode.CONCURRENT)
public final class DebianS3ITCase {

    /**
     * Curl exit code when resource not retrieved and `--fail` is used, http 400+.
     */
    private static final int CURL_NOT_FOUND = 22;

    /**
     * Artipie server port;
     */
    private static final int SRV_PORT = 8080;

    /**
     * Test repository name.
     */
    private static final String REPO = "my-debian";

    /**
     * S3 storage port.
     */
    private static final int S3_PORT = 9000;

    /**
     * Test deployments.
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("debian/debian-s3.yml", "my-debian")
            .withExposedPorts(DebianS3ITCase.SRV_PORT),
        () -> new TestDeployment.ClientContainer("artipie/deb-tests:1.0")
        .withWorkingDirectory("/w")
        .withNetworkAliases("minioc")
        .withExposedPorts(DebianS3ITCase.S3_PORT)
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

    @Test
    void curlPutWorks() throws Exception {
        this.containers.assertExec(
            "Packages.gz must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(DebianS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/%s/dists/my-debian/main/binary-amd64/Packages.gz"
                .formatted(DebianS3ITCase.S3_PORT, DebianS3ITCase.REPO).split(" ")
        );
        this.containers.assertExec(
            "aglfn_1.7-3_amd64.deb must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(DebianS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/%s/main/aglfn_1.7-3_amd64.deb"
                .formatted(DebianS3ITCase.S3_PORT, DebianS3ITCase.REPO).split(" ")
        );
        this.containers.assertExec(
            "Failed to upload deb package",
            new ContainerResultMatcher(),
            "timeout", "30s", "curl", "-i", "-X", "PUT", "--data-binary", "@/w/aglfn_1.7-3_amd64.deb",
            String.format("http://artipie:%s/%s/main/aglfn_1.7-3_amd64.deb",
                DebianS3ITCase.SRV_PORT, DebianS3ITCase.REPO
            )
        );
        this.containers.assertExec(
            "aglfn_1.7-3_amd64.deb must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minioc:%s/buck1/%s/main/aglfn_1.7-3_amd64.deb"
                .formatted(DebianS3ITCase.S3_PORT, DebianS3ITCase.REPO).split(" ")
        );
        this.containers.assertExec(
            "Packages.gz must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minioc:%s/buck1/%s/dists/my-debian/main/binary-amd64/Packages.gz"
                .formatted(DebianS3ITCase.S3_PORT, DebianS3ITCase.REPO).split(" ")
        );
        this.containers.assertExec(
            "deb from repo must be downloadable",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "timeout 30s curl -f -k http://artipie:%s/%s/main/aglfn_1.7-3_amd64.deb -o /home/aglfn_repo.deb"
                .formatted(DebianS3ITCase.SRV_PORT, DebianS3ITCase.REPO).split(" ")
        );
        this.containers.assertExec(
            "deb from repo must match with original",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "cmp /w/aglfn_1.7-3_amd64.deb /home/aglfn_repo.deb"
                .formatted(DebianS3ITCase.S3_PORT, DebianS3ITCase.REPO).split(" ")
        );
    }

    @Test
    void pushAndInstallWorks() throws Exception {
        this.containers.assertExec(
            "Packages.gz must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(DebianS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/%s/dists/my-debian/main/binary-amd64/Packages.gz"
                .formatted(DebianS3ITCase.S3_PORT, DebianS3ITCase.REPO).split(" ")
        );
        this.containers.assertExec(
            "aglfn_1.7-3_amd64.deb must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(DebianS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/%s/main/aglfn_1.7-3_amd64.deb"
                .formatted(DebianS3ITCase.S3_PORT, DebianS3ITCase.REPO).split(" ")
        );
        this.containers.putBinaryToClient(
            String.format(
                "deb [trusted=yes] http://artipie:%s/%s %s main",
                DebianS3ITCase.SRV_PORT, DebianS3ITCase.REPO, DebianS3ITCase.REPO
            ).getBytes(),
            "/etc/apt/sources.list"
        );
        this.containers.assertExec(
            "Failed to upload deb package",
            new ContainerResultMatcher(),
            "timeout", "30s", "curl", String.format("http://artipie:%s/%s/main/aglfn_1.7-3_amd64.deb",
                DebianS3ITCase.SRV_PORT, DebianS3ITCase.REPO),
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
            "curl -f -kv http://minioc:%s/buck1/%s/main/aglfn_1.7-3_amd64.deb"
                .formatted(DebianS3ITCase.S3_PORT, DebianS3ITCase.REPO).split(" ")
        );
        this.containers.assertExec(
            "Packages.gz must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minioc:%s/buck1/%s/dists/my-debian/main/binary-amd64/Packages.gz"
                .formatted(DebianS3ITCase.S3_PORT, DebianS3ITCase.REPO).split(" ")
        );
    }
}
