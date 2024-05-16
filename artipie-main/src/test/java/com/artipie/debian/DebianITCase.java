/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import org.cactoos.list.ListOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.BindMode;

/**
 * Debian integration test.
 * @since 0.15
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@Execution(ExecutionMode.CONCURRENT)
public final class DebianITCase {

    /**
     * Test deployments.
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("debian/debian.yml", "my-debian")
            .withRepoConfig("debian/debian-port.yml", "my-debian-port")
            .withExposedPorts(8081),
        () -> new TestDeployment.ClientContainer("artipie/deb-tests:1.0")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "debian/aglfn_1.7-3_amd64.deb", "/w/aglfn_1.7-3_amd64.deb", BindMode.READ_ONLY
            )
    );

    @ParameterizedTest
    @CsvSource({
        "8080,my-debian",
        "8081,my-debian-port"
    })
    void pushAndInstallWorks(final String port, final String repo) throws Exception {
        this.containers.putBinaryToClient(
            String.format(
                "deb [trusted=yes] http://artipie:%s/%s %s main", port, repo, repo
            ).getBytes(),
            "/etc/apt/sources.list"
        );
        this.containers.assertExec(
            "Failed to upload deb package",
            new ContainerResultMatcher(),
            "curl", String.format("http://artipie:%s/%s/main/aglfn_1.7-3_amd64.deb", port, repo),
            "--upload-file", "/w/aglfn_1.7-3_amd64.deb"
        );
        this.containers.assertExec(
            "Apt-get update failed",
            new ContainerResultMatcher(),
            "apt-get", "update"
        );
        this.containers.assertExec(
            "Package was not downloaded and unpacked",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(new ListOf<>("Unpacking aglfn", "Setting up aglfn"))
            ),
            "apt-get", "install", "-y", "aglfn"
        );
    }
}
