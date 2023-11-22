/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.file;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Integration test for binary repo.
 * @since 0.18
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class FileITCase {

    /**
     * Deployment for tests.
     * @checkstyle VisibilityModifierCheck (5 lines)
     * @checkstyle MagicNumberCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("binary/bin.yml", "bin")
            .withRepoConfig("binary/bin-port.yml", "bin-port")
            .withExposedPorts(8081),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.deployment.assertExec(
            "Failed to install deps",
            new ContainerResultMatcher(),
            "apk", "add", "--no-cache", "curl"
        );
    }

    @ParameterizedTest
    @CsvSource({
        "8080,bin",
        "8081,bin-port"
    })
    void canDownload(final String port, final String repo) throws Exception {
        final byte[] target = new byte[]{0, 1, 2, 3};
        this.deployment.putBinaryToArtipie(
            target, String.format("/var/artipie/data/%s/target", repo)
        );
        this.deployment.assertExec(
            "Failed to download artifact",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "curl", "-X", "GET", String.format("http://artipie:%s/%s/target", port, repo)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "8080,bin",
        "8081,bin-port"
    })
    void canUpload(final String port, final String repo) throws Exception {
        this.deployment.assertExec(
            "Failed to upload",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "curl", "-X", "PUT", "--data-binary", "123",
            String.format("http://artipie:%s/%s/target", port, repo)
        );
        this.deployment.assertArtipieContent(
            "Bad content after upload",
            String.format("/var/artipie/data/%s/target", repo),
            Matchers.equalTo("123".getBytes())
        );
    }

    @Test
    void repoWithPortIsNotAvailableByDefaultPort() throws IOException {
        this.deployment.assertExec(
            "Failed to upload",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS, new StringContains("HTTP/1.1 404 Not Found")
            ),
            "curl", "-i", "-X", "PUT", "--data-binary", "123", "http://artipie:8080/bin-port/target"
        );
        this.deployment.putBinaryToArtipie(
            "target".getBytes(StandardCharsets.UTF_8), "/var/artipie/data/bin-port/target"
        );
        this.deployment.assertExec(
            "Failed to download artifact",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS, new StringContains("not found")
            ),
            "curl", "-X", "GET", "http://artipie:8080/bin-port/target"
        );
    }
}
