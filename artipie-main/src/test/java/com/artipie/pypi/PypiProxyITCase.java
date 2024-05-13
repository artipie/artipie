/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.util.Map;
import org.cactoos.map.MapEntry;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Test to pypi proxy.
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
@Execution(ExecutionMode.CONCURRENT)
@Disabled
public final class PypiProxyITCase {

    /**
     * Test deployments.
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        Map.ofEntries(
            new MapEntry<>(
                "artipie",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("pypi-proxy/pypi.yml", "my-pypi")
                    .withUser("security/users/alice.yaml", "alice")
            ),
            new MapEntry<>(
                "artipie-proxy",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("pypi-proxy/pypi-proxy.yml", "my-pypi-proxy")
            )
        ),
        () -> new TestDeployment.ClientContainer("artipie/pypi-tests:1.0")
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
            new ContainerResultMatcher(
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
