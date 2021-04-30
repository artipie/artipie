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
import java.util.Arrays;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration test for {@link com.artipie.npm.proxy.http.NpmProxySlice}.
 * @since 0.13
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NpmProxyITCase {

    /**
     * Project name.
     */
    private static final String PROJ = "@hello/simple-npm-project";

    /**
     * Added npm project line.
     */
    private static final String ADDED_PROJ = String.format("+ %s@1.0.1", NpmProxyITCase.PROJ);

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        new MapOf<>(
            new MapEntry<>(
                "artipie",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("npm/npm.yml", "my-npm")
            ),
            new MapEntry<>(
                "artipie-proxy",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("npm/npm-proxy.yml", "my-npm-proxy")
            )
        ),
        () -> new TestDeployment.ClientContainer("node:14-alpine")
            .withWorkingDirectory("/w")
    );

    @Test
    void installFromProxy() throws Exception {
        this.containers.putBinaryToArtipie(
            "artipie",
            new TestResource(
                String.format("npm/storage/%s/meta.json", NpmProxyITCase.PROJ)
            ).asBytes(),
            String.format("/var/artipie/data/my-npm/%s/meta.json", NpmProxyITCase.PROJ)
        );
        final byte[] tgz = new TestResource(
            String.format("npm/storage/%s/-/%s-1.0.1.tgz", NpmProxyITCase.PROJ, NpmProxyITCase.PROJ)
        ).asBytes();
        this.containers.putBinaryToArtipie(
            "artipie", tgz,
            String.format(
                "/var/artipie/data/my-npm/%s/-/%s-1.0.1.tgz",
                NpmProxyITCase.PROJ, NpmProxyITCase.PROJ
            )
        );
        this.containers.assertExec(
            "Package was not installed",
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(
                    Arrays.asList(NpmProxyITCase.ADDED_PROJ, "added 1 package")
                )
            ),
            "npm", "install", NpmProxyITCase.PROJ, "--registry",
            "http://artipie-proxy:8080/my-npm-proxy"
        );
        this.containers.assertArtipieContent(
            "artipie-proxy",
            "Package was not cached in proxy",
            String.format(
                "/var/artipie/data/my-npm-proxy/%s/-/%s-1.0.1.tgz",
                NpmProxyITCase.PROJ, NpmProxyITCase.PROJ
            ),
            new IsEqual<>(tgz)
        );
    }

}
