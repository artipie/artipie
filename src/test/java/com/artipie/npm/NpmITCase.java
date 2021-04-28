/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.npm;

import com.artipie.asto.test.TestResource;
import com.artipie.maven.MavenITCase;
import com.artipie.test.TestDeployment;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import javax.json.Json;
import org.hamcrest.core.IsAnything;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Integration tests for Npm repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NpmITCase {

    /**
     * Artipie url.
     */
    private static final String REPO = "http://artipie:8080/my-npm";

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
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("npm/npm.yml", "my-npm"),
        () -> new TestDeployment.ClientContainer("node:14-alpine")
            .withWorkingDirectory("/w")
    );

    @Test
    void npmInstall() throws Exception {
        this.containers.putBinaryToArtipie(
            new TestResource(String.format("npm/storage/%s/meta.json", NpmITCase.PROJ)).asBytes(),
            String.format("/var/artipie/data/my-npm/%s/meta.json", NpmITCase.PROJ)
        );
        this.containers.putBinaryToArtipie(
            new TestResource(
                String.format("npm/storage/%s/-/%s-1.0.1.tgz", NpmITCase.PROJ, NpmITCase.PROJ)
            ).asBytes(),
            String.format(
                "/var/artipie/data/my-npm/%s/-/%s-1.0.1.tgz", NpmITCase.PROJ, NpmITCase.PROJ
            )
        );
        this.containers.assertExec(
            "Package was not installed",
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(
                    Arrays.asList(NpmITCase.ADDED_PROJ, "added 1 package")
                )
            ),
            "npm", "install", NpmITCase.PROJ, "--registry", NpmITCase.REPO
        );
        this.containers.assertExec(
            "Package was installed",
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0), new StringContains("@hello/simple-npm-project@1.0.1")
            ),
            "npm", "list"
        );
    }

    @Test
    void npmPublish() throws Exception {
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
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContains(NpmITCase.ADDED_PROJ)
            ),
            "npm", "publish", NpmITCase.PROJ, "--registry", NpmITCase.REPO
        );
        this.containers.assertArtipieContent(
            "Meta json is incorrect",
            String.format("/var/artipie/data/my-npm/%s/meta.json", NpmITCase.PROJ),
            new MatcherOf<>(
                bytes -> {
                    return Json.createReader(new ByteArrayInputStream(bytes)).readObject()
                        .getJsonObject("versions")
                        .getJsonObject("1.0.1")
                        .getJsonObject("dist")
                        .getString("tarball").equals(String.format("/%s", tgz));
                }
            )
        );
        this.containers.assertArtipieContent(
            "Tarball should be added to storage",
            String.format("/var/artipie/data/my-npm/%s", tgz),
            new IsAnything<>()
        );
    }

}
