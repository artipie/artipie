/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.test.TestDeployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
            .withUser("security/users/alice.yaml", "alice"),
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
        new TestDeployment.DockerTest(this.deployment, "artipie:8080")
            .loginAsAlice()
            .pull("alpine:3.11")
            .tag("alpine:3.11", image)
            .push(image)
            .remove(image)
            .pull(image)
            .assertExec();
    }
}
