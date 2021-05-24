/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.pypi;

import com.artipie.asto.test.TestResource;
import com.artipie.maven.MavenITCase;
import com.artipie.test.TestDeployment;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test to pypi proxy.
 * @since 0.12
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
@Disabled
public final class PypiProxyITCase {

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
                    .withRepoConfig("pypi-proxy/pypi.yml", "my-pypi")
                    .withCredentials("_credentials.yaml")
            ),
            new MapEntry<>(
                "artipie-proxy",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("pypi-proxy/pypi-proxy.yml", "my-pypi-proxy")
            )
        ),
        () -> new TestDeployment.ClientContainer("python:3")
            .withWorkingDirectory("/w")
    );

    @Test
    void installFromProxy() throws Exception {
        final byte[] data = new TestResource("pypi-repo/alarmtime-0.1.5.tar.gz").asBytes();
        this.containers.putBinaryToArtipie(
            "artipie", data,
            "/var/artipie/data/my-pypi/alarmtime/alarmtime-0.1.5.tar.gz"
        );
        this.containers.assertExec(
            "Package was not installed",
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0),
                Matchers.containsString("Successfully installed alarmtime-0.1.5")
            ),
            "pip", "install", "--no-deps", "--trusted-host", "artipie-proxy",
            "--index-url", "http://alice:123@artipie-proxy:8080/my-pypi-proxy/", "alarmtime"
        );
        this.containers.assertArtipieContent(
            "artipie-proxy",
            "/var/artipie/data/my-pypi-proxy/alarmtime/alarmtime-0.1.5.tar.gz",
            new IsEqual<>(data)
        );
    }

}
