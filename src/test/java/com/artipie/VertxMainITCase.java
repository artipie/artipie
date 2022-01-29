/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test for {@link VertxMain}.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class VertxMainITCase {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (15 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        new MapOf<>(
            new MapEntry<>(
                "artipie-config-key-present",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withConfig("artipie-repo-config-key.yaml")
                    .withRepoConfig("binary/bin.yml", "my_configs/my-file")
            ),
            new MapEntry<>(
                "artipie-invalid-repo-config",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("invalid_repo.yaml", "my-file")
            )
        ),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws IOException {
        this.deployment.assertExec(
            "Failed to install deps",
            new ContainerResultMatcher(),
            "apk", "add", "--no-cache", "curl"
        );
    }

    @Test
    void startsWhenNotValidRepoConfigsArePresent() throws IOException {
        this.deployment.putBinaryToArtipie(
            "artipie-invalid-repo-config",
            "Hello world".getBytes(),
            "/var/artipie/data/my-file/item.txt"
        );
        this.deployment.assertExec(
            "Artipie isn't started or not responding 200",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS,
                new StringContains("HTTP/1.1 200 OK")
            ),
            "curl", "-i", "-X", "GET",
            "http://artipie-invalid-repo-config:8080/my-file/item.txt"
        );
    }

    @Test
    void worksWhenRepoConfigsKeyIsPresent() throws IOException {
        this.deployment.putBinaryToArtipie(
            "artipie-config-key-present",
            "Hello world".getBytes(),
            "/var/artipie/data/my-file/item.txt"
        );
        this.deployment.assertExec(
            "Artipie isn't started or not responding 200",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS,
                new StringContains("HTTP/1.1 200 OK")
            ),
            "curl", "-i", "-X", "GET",
            "http://artipie-config-key-present:8080/my-file/item.txt"
        );
    }

}
