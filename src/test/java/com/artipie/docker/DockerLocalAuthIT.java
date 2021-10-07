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
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import wtf.g4s8.tuples.Pair;

/**
 * Integration test for auth in local Docker repositories.
 *
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
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
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("docker/registry-auth.yml", "registry")
            .withCredentials("_credentials.yaml"),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withPrivilegedMode(true)
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        // @checkstyle MethodBodyCommentsCheck (10 lines)
        // @checkstyle LineLengthCheck (10 lines)
        this.deployment.clientExec("apk", "add", "--update", "--no-cache", "openrc", "docker");
        // needs this command to initialize openrc directories on first call
        this.deployment.clientExec("rc-status");
        // this flag file is needed to tell openrc working in non-boot mode
        this.deployment.clientExec("touch", "/run/openrc/softlevel");
        // allow artipie:8080 insecure connection before starting docker daemon
        this.deployment.clientExec("sed", "-i", "s/DOCKER_OPTS=\"\"/DOCKER_OPTS=\"--insecure-registry=artipie:8080\"/g", "/etc/conf.d/docker");
        this.deployment.clientExec("rc-service", "docker", "start");
        // docker daemon needs some time to start after previous command
        this.deployment.clientExec("sleep", "3");
    }

    @Test
    void aliceCanPullAndPush() {
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

    @Test
    void canPullWithReadPermission() {
        final String image = "artipie:8080/registry/alpine:3.11";
        List.of(
            Pair.of(
                "Failed to login to Artipie as alice",
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
            Pair.of("Failed to logout from Artipie", List.of("docker", "logout", "artipie:8080")),
            Pair.of(
                "Failed to login to Artipie as bob",
                List.of(
                    "docker", "login",
                    "--username", "bob",
                    "--password", "qwerty",
                    "artipie:8080"
                )
            ),
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

    @Test
    void shouldFailPushIfNoWritePermission() throws Exception {
        final String image = "artipie:8080/registry/alpine:3.11";
        List.of(
            Pair.of(
                "Failed to login to Artipie",
                List.of(
                    "docker", "login",
                    "--username", "bob",
                    "--password", "qwerty",
                    "artipie:8080"
                )
            ),
            Pair.of("Failed to pull origin image", List.of("docker", "pull", "alpine:3.11")),
            Pair.of("Failed to tag origin image", List.of("docker", "tag", "alpine:3.11", image))
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
            Pair.of("Failed to pull origin image", List.of("docker", "pull", "alpine:3.11")),
            Pair.of("Failed to tag origin image", List.of("docker", "tag", "alpine:3.11", image))
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
            Pair.of("Failed to logout", List.of("docker", "logout", "artipie:8080"))
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
        this.deployment.assertExec(
            "Pull failed with unexpected status, should be 1",
            new ContainerResultMatcher(new IsEqual<>(1)),
            "docker", "pull", image
        );
    }

}
