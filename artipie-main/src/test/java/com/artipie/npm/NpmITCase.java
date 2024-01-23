/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsAnything;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Integration tests for Npm repository.
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NpmITCase {
    /**
     * Project name.
     */
    private static final String PROJ = "@hello/simple-npm-project";

    /**
     * Added npm project line.
     */
    private static final String ADDED_PROJ = String.format("+ %s@1.0.1", NpmITCase.PROJ);

    /**
     * Test deployments.
             */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("npm/npm.yml", "my-npm")
            .withRepoConfig("npm/npm-port.yml", "my-npm-port")
            .withExposedPorts(8081),
        () -> new TestDeployment.ClientContainer("node:14-alpine")
            .withWorkingDirectory("/w")
    );

    @ParameterizedTest
    @CsvSource({
        "8080,my-npm",
        "8081,my-npm-port"
    })
    void npmInstall(final String port, final String repo) throws Exception {
        this.containers.putBinaryToArtipie(
            new TestResource(String.format("npm/storage/%s/meta.json", NpmITCase.PROJ)).asBytes(),
            String.format("/var/artipie/data/%s/%s/meta.json", repo, NpmITCase.PROJ)
        );
        this.containers.putBinaryToArtipie(
            new TestResource(
                String.format("npm/storage/%s/-/%s-1.0.1.tgz", NpmITCase.PROJ, NpmITCase.PROJ)
            ).asBytes(),
            String.format(
                "/var/artipie/data/%s/%s/-/%s-1.0.1.tgz", repo, NpmITCase.PROJ, NpmITCase.PROJ
            )
        );
        this.containers.assertExec(
            "Package was not installed",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(
                    Arrays.asList(NpmITCase.ADDED_PROJ, "added 1 package")
                )
            ),
            "npm", "install", NpmITCase.PROJ, "--registry", this.repoUrl(port, repo)
        );
        this.containers.assertExec(
            "Package was installed",
            new ContainerResultMatcher(
                new IsEqual<>(0), new StringContains("@hello/simple-npm-project@1.0.1")
            ),
            "npm", "list"
        );
    }

    @ParameterizedTest
    @CsvSource({
        "8080,my-npm",
        "8081,my-npm-port"
    })
    void npmPublish(final String port, final String repo) throws Exception {
        final String tgz = String.format("%s/-/%s-1.0.1.tgz", NpmITCase.PROJ, NpmITCase.PROJ);
        this.containers.putBinaryToClient(
            new TestResource("npm/simple-npm-project/index.js").asBytes(),
            String.format("/w/%s/index.js", NpmITCase.PROJ)
        );
        this.containers.putBinaryToClient(
            new TestResource("npm/simple-npm-project/package.json").asBytes(),
            String.format("/w/%s/package.json", NpmITCase.PROJ)
        );
        this.containers.assertExec(
            "Package was published",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContains(NpmITCase.ADDED_PROJ)
            ),
            "npm", "publish", NpmITCase.PROJ, "--registry", this.repoUrl(port, repo)
        );
        final byte[] content = this.containers.getArtipieContent(
            String.format("/var/artipie/data/%s/%s/meta.json", repo, NpmITCase.PROJ)
        );
        MatcherAssert.assertThat(
            "Meta json is incorrect",
            Json.createReader(new ByteArrayInputStream(content)).readObject()
                .getJsonObject("versions")
                .getJsonObject("1.0.1")
                .getJsonObject("dist")
                .getString("tarball").equals(String.format("/%s", tgz))
        );
        this.containers.assertArtipieContent(
            "Tarball should be added to storage",
            String.format("/var/artipie/data/%s/%s", repo, tgz),
            new IsAnything<>()
        );
    }

    private String repoUrl(final String port, final String repo) {
        return String.format("http://artipie:%s/%s", port, repo);
    }
}
