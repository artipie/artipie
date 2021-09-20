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
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
//@Disabled(value = "FIXME: migrate artipie to testonctainers")
final class HelmITCase {

    /**
     * Chart name.
     */
    private static final String CHART = "tomcat-0.4.1.tgz";
//    private static final String CHART = "ark-1.0.1.tgz";

    /**
     * Repo name.
     */
    private static final String REPO = "my-helm";

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
        this.containers.assertExec(
            "Failed to install dependencies",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "apk", "add", "--no-cache", "curl"
        );
    }

    /**
     * Repository url.
     */
    private String url = "http://artipie:8080/my-helm";

    @Test
    void uploadChartAndCreateIndexYaml() throws Exception {
        final String chartrepo = "chartrepo";
//        this.containers.putBinaryToArtipie(
//            new TestResource("helm/tomcat-0.4.1.tgz").asBytes(),
//            "/var/artipie/data/my-helm/tomcat-0.4.1.tgz"
//        );
//        this.containers.putBinaryToArtipie(
////            new TestResource("helm/index.yaml").asBytes(),
//            "apiVersion: v1".getBytes(),
//            "/var/artipie/data/my-helm/index.yaml"
//        );

        this.containers.assertExec(
            "Failed to upload helm archive",
            new ContainerResultMatcher(new IsEqual<>(0)),
//            "curl", "-X", "PUT", String.format("http://artipie:8080/my-helm/%s", HelmITCase.CHART),
            "curl", "-X", "PUT", String.format("http://artipie:8080/my-helm/%s", "tomcat"),
//            "-H", "Content-Type: application/gzip",
//            "-H", "Content-Type:application/json;charset=UTF-8",
//            "--data-binary", String.format("/w/%s", HelmITCase.CHART),
//            "--data-binary", "@<(echo \"Uncompressed data\" | gzip)",
//            "--data-binary", HelmITCase.CHART,
            "-F", String.format("file=@/w/%s", HelmITCase.CHART),
            "--verbose"
//            "--data-binary", ""
//            new String(new TestResource(String.format("helm/%s", HelmITCase.CHART)).asBytes()),
        );
        this.containers.assertExec(
            "Init failed",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "helm", "init", "--stable-repo-url", this.url, "--client-only"
        );
        this.containers.assertExec(
            "Chart repository was not added",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "helm", "repo", "add", chartrepo, this.url
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
