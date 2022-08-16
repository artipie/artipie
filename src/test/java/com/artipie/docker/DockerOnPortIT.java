/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.settings.repo.RepoConfigYaml;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.nio.file.Path;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for local Docker repository running on port.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class DockerOnPortIT {

    /**
     * Temp directory.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @TempDir
    static Path temp;

    /**
     * Repository port.
     */
    private static final int PORT = 8085;

    /**
     * Example docker image to use in tests.
     */
    private Image image;

    /**
     * Docker repository.
     */
    private String repository;

    /**
     * Deployment for tests.
     *
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig(
                DockerOnPortIT.temp,
                new RepoConfigYaml("docker")
                    .withFileStorage(Path.of("/var/artipie/data/"))
                    .withPort(DockerOnPortIT.PORT)
                    .toString(),
                "my-docker"
            ),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withPrivilegedMode(true)
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.deployment.setUpForDockerTests(DockerOnPortIT.PORT);
        this.repository = String.format("artipie:%d", DockerOnPortIT.PORT);
        this.image = this.prepareImage();
        this.deployment.clientExec(
            "docker", "login",
            "--username", "alice",
            "--password", "123",
            this.repository
        );
    }

    @Test
    void shouldPush() throws Exception {
        this.deployment.assertExec(
            "Failed to push image",
            new ContainerResultMatcher(
                new IsEqual<>(ContainerResultMatcher.SUCCESS),
                new AllOf<>(
                    new StringContains(String.format("%s: Pushed", this.image.layer())),
                    new StringContains(String.format("latest: digest: %s", this.image.digest()))
                )
            ),
            "docker", "push", this.image.remote()
        );
    }

    @Test
    void shouldPullPushed() throws Exception {
        this.deployment.clientExec("docker", "push", this.image.remote());
        this.deployment.clientExec("docker", "image", "rm", this.image.name());
        this.deployment.clientExec("docker", "image", "rm", this.image.remote());
        this.deployment.assertExec(
            "Filed to pull image",
            new ContainerResultMatcher(
                new IsEqual<>(ContainerResultMatcher.SUCCESS),
                new StringContains(
                    String.format("Status: Downloaded newer image for %s", this.image.remote())
                )
            ),
            "docker", "pull", this.image.remote()
        );
    }

    private Image prepareImage() throws Exception {
        final Image source = new Image.ForOs();
        this.deployment.clientExec("docker", "pull", source.remoteByDigest());
        this.deployment.clientExec("docker", "tag", source.remoteByDigest(), "my-test:latest");
        final Image img = new Image.From(
            this.repository,
            "my-test",
            source.digest(),
            source.layer()
        );
        this.deployment.clientExec("docker", "tag", source.remoteByDigest(), img.remote());
        return img;
    }
}
