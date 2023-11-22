/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.util.Arrays;
import java.util.Map;
import org.cactoos.map.MapEntry;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
     * @checkstyle MagicNumberCheck (15 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        Map.ofEntries(
            new MapEntry<>(
                "artipie",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("npm/npm.yml", "my-npm")
            ),
            new MapEntry<>(
                "artipie-proxy",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("npm/npm-proxy.yml", "my-npm-proxy")
                    .withRepoConfig("npm/npm-proxy-port.yml", "my-npm-proxy-port")
                    .withExposedPorts(8081)
            )
        ),
        () -> new TestDeployment.ClientContainer("node:14-alpine")
            .withWorkingDirectory("/w")
    );

    @ParameterizedTest
    @CsvSource({
        "8080,my-npm-proxy",
        "8081,my-npm-proxy-port"
    })
    void installFromProxy(final String port, final String repo) throws Exception {
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
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(
                    Arrays.asList(NpmProxyITCase.ADDED_PROJ, "added 1 package")
                )
            ),
            "npm", "install", NpmProxyITCase.PROJ, "--registry",
            String.format("http://artipie-proxy:%s/%s", port, repo)
        );
        this.containers.assertArtipieContent(
            "artipie-proxy",
            "Package was not cached in proxy",
            String.format(
                "/var/artipie/data/%s/%s/-/%s-1.0.1.tgz",
                repo, NpmProxyITCase.PROJ, NpmProxyITCase.PROJ
            ),
            new IsEqual<>(tgz)
        );
    }

}
