/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.file;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration test for binary repo.
 * @since 0.18
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class FileITCase {

    /**
     * Deployment for tests.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("binary/bin.yml", "bin"),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.deployment.assertExec(
            "Failed to install deps",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "apk", "add", "--no-cache", "curl"
        );
    }

    @Test
    void canDownload() throws Exception {
        final byte[] target = new byte[]{0, 1, 2, 3};
        this.deployment.putBinaryToArtipie(target, "/var/artipie/data/bin/target");
        this.deployment.assertExec(
            "Failed to download artifact",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "curl", "-X", "GET", "http://artipie:8080/bin/target"
        );
    }

    @Test
    void canUpload() throws Exception {
        this.deployment.assertExec(
            "Failed to upload",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "curl", "-X", "PUT", "--data-binary", "123", "http://artipie:8080/bin/test"
        );
        this.deployment.assertArtipieContent(
            "Bad content after upload",
            "/var/artipie/data/bin/test",
            Matchers.equalTo("123".getBytes())
        );
    }
}
