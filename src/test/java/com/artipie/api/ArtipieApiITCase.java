/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * IT for Artipie API and dashboard.
 * @since 0.14
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ArtipieApiITCase {

    /**
     * Deployment for tests.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        () -> new TestDeployment.ArtipieContainer().withConfig("artipie_org.yaml")
            .withCredentials("_credentials.yaml")
            .withPermissions("_permissions.yaml"),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.deployment.assertExec(
            "Failed to install deps",
            new ContainerResultMatcher(Matchers.is(0)),
            "apk", "add", "--no-cache", "curl"
        );
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void createRepository() throws Exception {
        final String repo = "repo1";
        final String config = String.join(
            "\n",
            "repo:",
            "  type: file",
            "  storage:",
            "    type: fs",
            "    path: /var/artipie/repo/1"
        );
        this.deployment.assertExec(
            "Artipie dashboard is not up and running",
            new ContainerResultMatcher(Matchers.is(0)),
            "curl", "-X", "GET", "http://artipie:8080/dashboard/alice", "-u", "alice:123"
        );
        this.deployment.assertExec(
            "Failed to create a new repo",
            new ContainerResultMatcher(Matchers.is(1)),
            "curl", "-X", "POST", "http://artipie:8080/api/repos/alice",
            "-u", "alice:123",
            "--data", String.format("repo=%s;config=%s", repo, config)
        );
    }
}
