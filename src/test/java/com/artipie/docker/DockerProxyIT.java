/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.docker.proxy.ProxyDocker;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration test for {@link ProxyDocker}.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.10
 * @todo #499:30min Add integration test for Docker proxy cache feature.
 *  Docker proxy supports caching feature for it's remote repositories.
 *  Cache is populated when image is downloaded asynchronously
 *  and later used if remote repository is unavailable.
 *  This feature should be tested.
 * @todo #499:30min Add integration test for Docker proxy push feature.
 *  Docker proxy supports pushing to local storage if such storage is specified.
 *  It should be verified that an image can be pushed to proxy repository and pulled later.
 * @todo #449:30min Support running DockerProxyIT test on Windows.
 *  Running test on Windows uses `mcr.microsoft.com/dotnet/core/runtime` image.
 *  Loading this image manifest fails with
 *  "java.lang.IllegalStateException: multiple subscribers not supported" error.
 *  It seems that body is being read by some other entity in Artipie,
 *  so it requires investigation.
 *  Similar `CachingProxyITCase` tests works well in docker-adapter module.
 *  @todo #996:30min Refactor set up of a test's steps.
 *   Consider creating a class of docker test to avoid code duplication in
 *   DockerProxyIT and DockerLocalITCase tests.
 *   So this test could be rewriting follow way:
 *   {@code new DockerTest(this.deployment, "artipie:8080")
 *             .login("alice", "123")
 *             .pull(
 *                 img,
 *                 new ContainerResultMatcher(
 *                     new IsEqual<>(ContainerResultMatcher.SUCCESS),
 *                     new StringContains(
 *                      String.format("Status: Downloaded newer image for %s", img)
 *                     )
 *                 )
 *             )
 *             .assertExec();}
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs(OS.LINUX)
final class DockerProxyIT {

    /**
     * Deployment for tests.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("docker/docker-proxy.yml", "my-docker"),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withPrivilegedMode(true)
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.deployment.setUpForDockerTests();
    }

    @Test
    void shouldPullRemote() throws Exception {
        final Image image = new Image.ForOs();
        final String img = new Image.From(
            "artipie:8080",
            String.format("my-docker/%s", image.name()),
            image.digest(),
            image.layer()
        ).remoteByDigest();
        this.deployment.assertExec(
            "Failed to login to Artipie",
            new ContainerResultMatcher(),
            "docker", "login",
            "--username", "alice",
            "--password", "123",
            "artipie:8080"
        );
        this.deployment.assertExec(
            "Failed to pull image",
            new ContainerResultMatcher(
                new IsEqual<>(ContainerResultMatcher.SUCCESS),
                new StringContains(String.format("Status: Downloaded newer image for %s", img))
            ),
            "docker", "pull", img
        );
    }
}
