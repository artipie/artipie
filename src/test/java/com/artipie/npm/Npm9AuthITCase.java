/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.Container;

/**
 * Integration tests for Npm repository with npm client version 9 and token auth.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class Npm9AuthITCase {
    /**
     * Project name.
     */
    private static final String PROJ = "@hello/simple-npm-project";

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     * @checkstyle MagicNumberCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> new TestDeployment.ArtipieContainer().withConfig("artipie_with_policy.yaml")
            .withRepoConfig("npm/npm-auth.yml", "my-npm")
            .withUser("security/users/alice.yaml", "alice"),
        () -> new TestDeployment.ClientContainer("node:19-alpine")
            .withWorkingDirectory("/w")
    );

    @Test
    void aliceCanUploadAndInstall() throws Exception {
        this.obtainAuthToken();
        this.addFilesToPublish();
        this.containers.assertExec(
            "Package was published",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContains("+ @hello/simple-npm-project@1.0.1")
            ),
            "npm", "publish", "@hello/simple-npm-project/",
            "--registry", "http://artipie:8080/my-npm"
        );
        this.containers.assertExec(
            "Package was not installed",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContains("added 1 package")
            ),
            "npm", "install", Npm9AuthITCase.PROJ, "--registry", "http://artipie:8080/my-npm"
        );
        this.containers.assertExec(
            "Package was installed",
            new ContainerResultMatcher(
                new IsEqual<>(0), new StringContains("@hello/simple-npm-project@1.0.1")
            ),
            "npm", "list"
        );
    }

    @Test
    void failsToPublishAndInstallWithInvalidToken() throws IOException {
        this.containers.putBinaryToClient(
            "//artipie:8080/:_authToken=abc123".getBytes(StandardCharsets.UTF_8),
            "/w/.npmrc"
        );
        this.addFilesToPublish();
        this.containers.assertExec(
            "Package was published",
            new ContainerResultMatcher(
                new IsEqual<>(1),
                new StringContains("Unable to authenticate")
            ),
            "npm", "publish", "@hello/simple-npm-project/",
            "--registry", "http://artipie:8080/my-npm"
        );
        this.containers.assertExec(
            "Package was not installed",
            new ContainerResultMatcher(
                new IsEqual<>(1),
                new StringContains("Unable to authenticate")
            ),
            "npm", "install", Npm9AuthITCase.PROJ, "--registry", "http://artipie:8080/my-npm"
        );
    }

    private void addFilesToPublish() {
        this.containers.putBinaryToClient(
            new TestResource("npm/simple-npm-project/index.js").asBytes(),
            String.format("/w/%s/index.js", Npm9AuthITCase.PROJ)
        );
        this.containers.putBinaryToClient(
            new TestResource("npm/simple-npm-project/package.json").asBytes(),
            String.format("/w/%s/package.json", Npm9AuthITCase.PROJ)
        );
    }

    private void obtainAuthToken() throws IOException {
        this.containers.exec("apk", "add", "--no-cache", "curl");
        final Container.ExecResult res = this.containers.exec(
            "curl", "-X", "POST", "-d", "{\"name\":\"alice\",\"pass\":\"123\"}",
            "-H", "Content-type: application/json",
            "http://artipie:8086/api/v1/oauth/token"
        );
        this.containers.putBinaryToClient(
            String.format(
                "//artipie:8080/:_authToken=%s",
                Json.createReader(new StringReader(res.getStdout())).readObject().getString("token")
            ).getBytes(StandardCharsets.UTF_8),
            "/w/.npmrc"
        );
    }

}
