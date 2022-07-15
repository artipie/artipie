/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.helm;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;

/**
 * Integration tests for Helm repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @todo #607:60min Verify install using kubectl in docker.
 *  Now test just check that `index.yaml` was created. It would
 *  be better to verify install within `helm install`. For this,
 *  it's necessary to create a kubernetes cluster in Docker.
 * @since 0.13
 * @todo #1041:30min Add test cases with repository on individual port: create one more
 *  repository with `port` settings and start it in Artipie container exposing the port with
 *  `withExposedPorts` method. Then, parameterize test cases to check repositories with different
 *  ports. Check `FileITCase` as an example.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class HelmITCase {

    /**
     * Chart name.
     */
    private static final String CHART = "tomcat-0.4.1.tgz";

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("helm/my-helm.yml", "my-helm"),
        () -> new TestDeployment.ClientContainer("alpine/helm:2.16.9")
            .withWorkingDirectory("/w")
            .withCreateContainerCmdModifier(
                cmd -> cmd.withEntrypoint("/bin/sh")
                    .withCmd("-c", "while true; do sleep 300; done;")
            ).withClasspathResourceMapping(
                String.format("helm/%s", HelmITCase.CHART),
                String.format("/w/%s", HelmITCase.CHART),
                BindMode.READ_ONLY
            )
    );

    @BeforeEach
    void setUp() throws Exception {
        this.containers.clientExec("apk", "add", "--no-cache", "curl");
    }

    @Test
    void uploadChartAndCreateIndexYaml() throws Exception {
        final String url = "http://artipie:8080/my-helm";
        this.containers.assertExec(
            "Failed to upload helm archive",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl", "-X", "PUT", String.format("%s/%s", url, "tomcat"),
            "--upload-file", String.format("/w/%s", HelmITCase.CHART),
            "--verbose"
        );
        this.containers.assertExec(
            "Init failed",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "helm", "init", "--stable-repo-url", url, "--client-only"
        );
        this.containers.assertExec(
            "Chart repository was not added",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "helm", "repo", "add", "chartrepo", url
        );
        this.containers.assertExec(
            "Repo update from chart repository failed",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContains("Update Complete.")
            ),
            "helm", "repo", "update"
        );
    }
}
