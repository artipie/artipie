/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.docker.proxy.ProxyDocker;
import com.artipie.test.TestDeployment;
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
 * @todo #449:30min Support running DockerProxyIT test on Windows.
 *  Running test on Windows uses `mcr.microsoft.com/dotnet/core/runtime` image.
 *  Loading this image manifest fails with
 *  "java.lang.IllegalStateException: multiple subscribers not supported" error.
 *  It seems that body is being read by some other entity in Artipie,
 *  so it requires investigation.
 *  Similar `CachingProxyITCase` tests works well in docker-adapter module.
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
            .withUser("security/users/alice.yaml", "alice")
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
        new TestDeployment.DockerTest(this.deployment, "artipie:8080")
            .loginAsAlice()
            .pull(
                img,
                new StringContains(
                    String.format("Status: Downloaded newer image for %s", img)
                )
            )
            .assertExec();
    }

    @Test
    void shouldPushAndPull() throws Exception {
        final String image = "artipie:8080/my-docker/alpine:3.11";
        new TestDeployment.DockerTest(this.deployment, "artipie:8080")
            .loginAsAlice()
            .pull(
                "alpine:3.11",
                new StringContains(
                    "Status: Downloaded newer image for alpine:3.11"
                )
            )
            .tag("alpine:3.11", image)
            .push(
                image,
                new StringContains(
                    "The push refers to repository [artipie:8080/my-docker/alpine]"
                )
            )
            .remove(
                image,
                new StringContains("Untagged: artipie:8080/my-docker/alpine:3.11")
            )
            .pull(
                image,
                new StringContains(
                    "Downloaded newer image for artipie:8080/my-docker/alpine:3.11"
                )
            )
            .assertExec();
    }
}
