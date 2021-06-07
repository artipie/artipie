/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * IT for Artipie API and dashboard.
 * @since 0.14
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
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
    void dashboardIsUp() throws IOException {
        this.deployment.assertExec(
            "Artipie dashboard is not up and running",
            new ContainerResultMatcher(
                Matchers.is(0),
                new StringContainsInOrder(
                    new ListOf<String>(
                        "<!DOCTYPE html>", "<title>alice</title>", "Your repositories:"
                    )
                )
            ),
            "curl", "-X", "GET", "http://artipie:8080/dashboard/alice", "-u", "alice:123"
        );
    }

    @Test
    @Disabled
    void createRepository() throws Exception {
        final String repo = "repo1";
        final String config = this.config();
        this.deployment.assertExec(
            "Failed to create a new repo",
            new ContainerResultMatcher(Matchers.is(0), new StringContains("<!DOCTYPE html>")),
            "curl", "-X", "POST", "http://artipie:8080/api/repos/alice",
            "-u", "alice:123",
            "--data", String.format("repo=%s;config=%s", repo, config)
        );
        this.deployment.assertArtipieContent(
            "Repo config is wrong",
            String.format("/var/artipie/repo/alice/%s.yaml", repo),
            new IsEqual<>(config.getBytes())
        );
    }

    @Test
    @Disabled
    void updatesRepository() throws Exception {
        final String repo = "repo1";
        String config = this.config();
        this.deployment.putBinaryToArtipie(
            config.getBytes(), String.format("/var/artipie/repo/alice/%s.yaml", repo)
        );
        config = config.replace("/var/artipie/repo/1", "/var/artipie/repo/one");
        this.deployment.assertExec(
            "Failed to create a new repo",
            new ContainerResultMatcher(Matchers.is(0), new StringContains("<!DOCTYPE html>")),
            "curl", "-X", "POST", "http://artipie:8080/api/repos/alice",
            "-u", "alice:123",
            "--data", String.format("repo=%s;config=%s", repo, config)
        );
        this.deployment.assertArtipieContent(
            "Repo config is wrong",
            String.format("/var/artipie/repo/alice/%s.yaml", repo),
            new IsEqual<>(config.getBytes())
        );
    }

    private String config() {
        return String.join(
            "\n",
            "repo:",
            "  type: file",
            "  storage:",
            "    type: fs",
            "    path: /var/artipie/repo/1"
        );
    }
}
