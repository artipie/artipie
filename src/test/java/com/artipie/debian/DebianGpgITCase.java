/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;

/**
 * Debian integration test.
 * @since 0.17
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class DebianGpgITCase {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("debian/debian-gpg.yml", "my-debian")
            .withClasspathResourceMapping(
                "debian/secret-keys.gpg", "/var/artipie/repo/secret-keys.gpg", BindMode.READ_ONLY
            ),
        () -> new TestDeployment.ClientContainer("debian:10.8")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "debian/aglfn_1.7-3_amd64.deb", "/w/aglfn_1.7-3_amd64.deb", BindMode.READ_ONLY
            )
            .withClasspathResourceMapping(
                "debian/public-key.asc", "/w/public-key.asc", BindMode.READ_ONLY
            )
    );

    @BeforeEach
    void setUp() throws IOException {
        this.containers.assertExec(
            "Apt-get update failed",
            new ContainerResultMatcher(),
            "apt-get", "update"
        );
        this.containers.assertExec(
            "Failed to install curl",
            new ContainerResultMatcher(),
            "apt-get", "install", "-y", "curl"
        );
        this.containers.assertExec(
            "Failed to install gnupg",
            new ContainerResultMatcher(),
            "apt-get", "install", "-y", "gnupg"
        );
        this.containers.assertExec(
            "Failed to add public key to apt-get",
            new ContainerResultMatcher(),
            "apt-key", "add", "/w/public-key.asc"
        );
        this.containers.putBinaryToClient(
            "deb http://artipie:8080/my-debian my-debian main".getBytes(),
            "/etc/apt/sources.list"
        );
    }

    @Test
    void pushAndInstallWorks() throws Exception {
        this.containers.assertExec(
            "Failed to upload deb package",
            new ContainerResultMatcher(),
            "curl", "http://artipie:8080/my-debian/main/aglfn_1.7-3_amd64.deb",
            "--upload-file", "/w/aglfn_1.7-3_amd64.deb"
        );
        this.containers.assertExec(
            "Apt-get update failed",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new AllOf<String>(
                    new ListOf<Matcher<? super String>>(
                        new StringContains(
                            "Get:1 http://artipie:8080/my-debian my-debian InRelease"
                        ),
                        new StringContains(
                            "Get:2 http://artipie:8080/my-debian my-debian/main amd64 Packages"
                        ),
                        new IsNot<>(new StringContains("Get:3"))
                    )
                )
            ),
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
