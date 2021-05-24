/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.pypi;

import com.artipie.maven.MavenITCase;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.hamcrest.Matchers;
import org.hamcrest.text.StringContainsInOrder;
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
        final String meta = "pypi-repo/example-pckg/dist/artipietestpkg-0.0.3.tar.gz";
        this.containers.putResourceToArtipie(
            meta, "/var/artipie/data/my-python/artipietestpkg/artipietestpkg-0.0.3.tar.gz"
        );
        this.containers.assertExec(
            "Failed to install package",
            new MavenITCase.ContainerResultMatcher(
                Matchers.equalTo(0),
                new StringContainsInOrder(
                    new ListOf<>(
                        "Looking in indexes: http://artipie:8080/my-python",
                        "Collecting artipietestpkg",
                        String.format(
                            "  Downloading http://artipie:8080/my-python/artipietestpkg/%s",
                            "artipietestpkg-0.0.3.tar.gz"
                        ),
                        "Building wheels for collected packages: artipietestpkg",
                        "  Building wheel for artipietestpkg (setup.py): started",
                        String.format(
                            "  Building wheel for artipietestpkg (setup.py): %s",
                            "finished with status 'done'"
                        ),
                        "Successfully built artipietestpkg",
                        "Installing collected packages: artipietestpkg",
                        "Successfully installed artipietestpkg-0.0.3"
                    )
                )
            ),
            "python", "-m", "pip", "install", "--trusted-host", "artipie", "--index-url",
            "http://artipie:8080/my-python", "artipietestpkg"
        );
    }

    @Test
    void canUpload() throws Exception {
        this.containers.assertExec(
            "Failed to upload",
            new MavenITCase.ContainerResultMatcher(
                Matchers.is(0),
                new StringContainsInOrder(
                    new ListOf<>(
                        "Uploading artipietestpkg-0.0.3-py2-none-any.whl", "100%",
                        "Uploading artipietestpkg-0.0.3.tar.gz", "100%"
                    )
                )
            ),
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
