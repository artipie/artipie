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
package com.artipie.debian;

import com.artipie.maven.MavenITCase;
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
 * @since 0.15
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
        () -> new TestDeployment.ClientContainer("debian")
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
            "apt-get update successful",
            new MavenITCase.ContainerResultMatcher(),
            "apt-get", "update"
        );
        this.containers.assertExec(
            "Failed to install curl",
            new MavenITCase.ContainerResultMatcher(),
            "apt-get", "install", "-y", "curl"
        );
        this.containers.assertExec(
            "Failed to install gnupg",
            new MavenITCase.ContainerResultMatcher(),
            "apt-get", "install", "-y", "gnupg"
        );
        this.containers.assertExec(
            "Failed to add public key to apt-get",
            new MavenITCase.ContainerResultMatcher(),
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
            new MavenITCase.ContainerResultMatcher(),
            "curl", "http://artipie:8080/my-debian/main/aglfn_1.7-3_amd64.deb",
            "--upload-file", "/w/aglfn_1.7-3_amd64.deb"
        );
        this.containers.assertExec(
            "Apt-get update failed",
            new MavenITCase.ContainerResultMatcher(
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
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(new ListOf<>("Unpacking aglfn", "Setting up aglfn"))
            ),
            "apt-get", "install", "-y", "aglfn"
        );
    }
}
