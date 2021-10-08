/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import wtf.g4s8.tuples.Pair;

/**
 * Integration test for local Docker repositories.
 *
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class DockerLocalITCase {

    /**
     * Deployment for tests.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("docker/registry.yml", "registry")
            .withCredentials("_credentials.yaml"),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withPrivilegedMode(true)
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.deployment.setUpForDockerTests();
    }

    @Test
    void pushAndPull() throws Exception {
        final String image = "artipie:8080/registry/alpine:3.11";
        List.of(
            Pair.of(
                "Failed to login to Artipie",
                List.of(
                    "docker", "login",
                    "--username", "alice",
                    "--password", "123",
                    "artipie:8080"
                )
            ),
            Pair.of("Failed to pull origin image", List.of("docker", "pull", "alpine:3.11")),
            Pair.of("Failed to tag origin image", List.of("docker", "tag", "alpine:3.11", image)),
            Pair.of("Failed to push image to Artipie", List.of("docker", "push", image)),
            Pair.of("Failed to remove local image", List.of("docker", "image", "rm", image)),
            Pair.of("Failed to pull image from Artipie", List.of("docker", "pull", image))
        ).forEach(
            pair -> pair.accept(
                (msg, cmds) -> {
                    try {
                        this.deployment.assertExec(
                            msg, new ContainerResultMatcher(), cmds
                        );
                    } catch (final IOException err) {
                        throw new UncheckedIOException(err);
                    }
                }
            )
        );
    }
}
