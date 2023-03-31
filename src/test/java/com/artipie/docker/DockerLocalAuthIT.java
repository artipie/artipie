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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration test for auth in local Docker repositories.
 *
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle LineLengthCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
final class DockerLocalAuthIT {

    /**
     * Deployment for tests.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        () -> new TestDeployment.ArtipieContainer().withConfig("artipie_with_policy.yaml")
            .withRepoConfig("docker/registry-auth.yml", "registry")
            .withUser("security/users/alice.yaml", "alice")
            .withUser("security/users/bob.yaml", "bob")
            .withRole("security/roles/readers.yaml", "readers"),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withPrivilegedMode(true)
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.deployment.setUpForDockerTests();
    }

    @Test
    void aliceCanPullAndPush() {
        final String image = "artipie:8080/registry/alpine:3.11";
        List.of(
            ImmutablePair.of(
                "Failed to login to Artipie",
                List.of(
                    "docker", "login",
                    "--username", "alice",
                    "--password", "123",
                    "artipie:8080"
                )
            ),
            ImmutablePair.of("Failed to pull origin image", List.of("docker", "pull", "alpine:3.11")),
            ImmutablePair.of("Failed to tag origin image", List.of("docker", "tag", "alpine:3.11", image)),
            ImmutablePair.of("Failed to push image to Artipie", List.of("docker", "push", image)),
            ImmutablePair.of("Failed to remove local image", List.of("docker", "image", "rm", image)),
            ImmutablePair.of("Failed to pull image from Artipie", List.of("docker", "pull", image))
        ).forEach(this::assertExec);
    }

    @Test
    void canPullWithReadPermission() {
        final String image = "artipie:8080/registry/alpine:3.11";
        List.of(
            ImmutablePair.of(
                "Failed to login to Artipie as alice",
                List.of(
                    "docker", "login",
                    "--username", "alice",
                    "--password", "123",
                    "artipie:8080"
                )
            ),
            ImmutablePair.of("Failed to pull origin image", List.of("docker", "pull", "alpine:3.11")),
            ImmutablePair.of("Failed to tag origin image", List.of("docker", "tag", "alpine:3.11", image)),
            ImmutablePair.of("Failed to push image to Artipie", List.of("docker", "push", image)),
            ImmutablePair.of("Failed to remove local image", List.of("docker", "image", "rm", image)),
            ImmutablePair.of("Failed to logout from Artipie", List.of("docker", "logout", "artipie:8080")),
            ImmutablePair.of(
                "Failed to login to Artipie as bob",
                List.of(
                    "docker", "login",
                    "--username", "bob",
                    "--password", "qwerty",
                    "artipie:8080"
                )
            ),
            ImmutablePair.of("Failed to pull image from Artipie", List.of("docker", "pull", image))
        ).forEach(this::assertExec);
    }

    @Test
    void shouldFailPushIfNoWritePermission() throws Exception {
        final String image = "artipie:8080/registry/alpine:3.11";
        List.of(
            ImmutablePair.of(
                "Failed to login to Artipie",
                List.of(
                    "docker", "login",
                    "--username", "bob",
                    "--password", "qwerty",
                    "artipie:8080"
                )
            ),
            ImmutablePair.of("Failed to pull origin image", List.of("docker", "pull", "alpine:3.11")),
            ImmutablePair.of("Failed to tag origin image", List.of("docker", "tag", "alpine:3.11", image))
        ).forEach(this::assertExec);
        this.deployment.assertExec(
            "Push failed with unexpected status, should be 1",
            new ContainerResultMatcher(new IsEqual<>(1)),
            "docker", "push", image
        );
    }

    @Test
    void shouldFailPushIfAnonymous() throws IOException {
        final String image = "artipie:8080/registry/alpine:3.11";
        List.of(
            ImmutablePair.of("Failed to pull origin image", List.of("docker", "pull", "alpine:3.11")),
            ImmutablePair.of("Failed to tag origin image", List.of("docker", "tag", "alpine:3.11", image))
        ).forEach(this::assertExec);
        this.deployment.assertExec(
            "Push failed with unexpected status, should be 1",
            new ContainerResultMatcher(new IsEqual<>(1)),
            "docker", "push", image
        );
    }

    @Test
    void shouldFailPullIfAnonymous() throws IOException {
        final String image = "artipie:8080/registry/alpine:3.11";
        List.of(
            ImmutablePair.of(
                "Failed to login to Artipie",
                List.of(
                    "docker", "login",
                    "--username", "alice",
                    "--password", "123",
                    "artipie:8080"
                )
            ),
            ImmutablePair.of("Failed to pull origin image", List.of("docker", "pull", "alpine:3.11")),
            ImmutablePair.of("Failed to tag origin image", List.of("docker", "tag", "alpine:3.11", image)),
            ImmutablePair.of("Failed to push image to Artipie", List.of("docker", "push", image)),
            ImmutablePair.of("Failed to remove local image", List.of("docker", "image", "rm", image)),
            ImmutablePair.of("Failed to logout", List.of("docker", "logout", "artipie:8080"))
        ).forEach(this::assertExec);
        this.deployment.assertExec(
            "Pull failed with unexpected status, should be 1",
            new ContainerResultMatcher(new IsEqual<>(1)),
            "docker", "pull", image
        );
    }

    private void assertExec(final ImmutablePair<String, List<String>> pair) {
        try {
            this.deployment.assertExec(
                pair.getKey(), new ContainerResultMatcher(), pair.getValue()
            );
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }
}
