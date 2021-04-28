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
package com.artipie.pypi;

import com.artipie.maven.MavenITCase;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;

/**
 * Integration tests for Pypi repository.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class PypiITCase {
    /**
     * Test deployments.
     *
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("pypi-repo/pypi.yml", "my-python"),
        () -> new TestDeployment.ClientContainer("python:3.7")
            .withWorkingDirectory("/var/artipie")
            .withClasspathResourceMapping(
                "pypi-repo/example-pckg",
                "/var/artipie/data/artipie/pypi/example-pckg",
                BindMode.READ_ONLY
            )
    );

    @BeforeEach
    void setUp() throws IOException {
        this.containers.assertExec(
            "Apt-get update failed",
            new MavenITCase.ContainerResultMatcher(),
            "apt-get", "update"
        );
        this.containers.assertExec(
            "Failed to install twine",
            new MavenITCase.ContainerResultMatcher(),
            "python", "-m", "pip", "install", "twine"
        );
        this.containers.assertExec(
            "Failed to upgrade pip",
            new MavenITCase.ContainerResultMatcher(),
            "python", "-m", "pip", "install", "--upgrade", "pip"
        );
    }

    @Test
    void installPythonPackage() throws IOException {
        String meta = "pypi-repo/example-pckg/dist/artipietestpkg-0.0.3.tar.gz";
        this.containers.putResourceToArtipie(
            meta, "/var/artipie/data/my-python/artipietestpkg/artipietestpkg-0.0.3.tar.gz"
        );
        meta = "pypi-repo/example-pckg/dist/artipietestpkg-0.0.3-py2-none-any.whl";
        this.containers.putResourceToArtipie(
            meta,
            "/var/artipie/data/my-python/artipietestpkg/artipietestpkg-0.0.3-py2-none-any.whl"
        );
        this.containers.assertExec(
            "Failed to install package",
            new MavenITCase.ContainerResultMatcher(Matchers.equalTo(0)),
            "python", "-m", "pip", "install", "--trusted-host", "artipie", "--index-url",
            "http://artipie:8080/my-python", "artipietestpkg"
        );
    }

    @Test
    void canUpload() throws Exception {
        this.containers.assertExec(
            "Failed to upload",
            new MavenITCase.ContainerResultMatcher(Matchers.is(0)),
            "python3", "-m", "twine", "upload", "--repository-url",
            "http://artipie:8080/my-python/", "-u", "alice", "-p", "123",
            "/var/artipie/data/artipie/pypi/example-pckg/dist/*"
        );
        this.containers.assertArtipieContent(
            "Bad content after upload",
            "/var/artipie/data/my-python/artipietestpkg/artipietestpkg-0.0.3.tar.gz",
            Matchers.not("123".getBytes())
        );
    }
}
