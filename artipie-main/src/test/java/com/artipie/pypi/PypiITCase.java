/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.hamcrest.Matchers;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
     * @checkstyle MagicNumberCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("pypi-repo/pypi.yml", "my-python")
            .withRepoConfig("pypi-repo/pypi-port.yml", "my-python-port")
            .withExposedPorts(8081),

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
            new ContainerResultMatcher(),
            "apt-get", "update"
        );
        this.containers.assertExec(
            "Failed to install twine",
            new ContainerResultMatcher(),
            "python", "-m", "pip", "install", "twine"
        );
        this.containers.assertExec(
            "Failed to upgrade pip",
            new ContainerResultMatcher(),
            "python", "-m", "pip", "install", "--upgrade", "pip"
        );
    }

    @ParameterizedTest
    @CsvSource({
        "8080,my-python",
        "8081,my-python-port"
    })
    void installPythonPackage(final String port, final String repo) throws IOException {
        final String meta = "pypi-repo/example-pckg/dist/artipietestpkg-0.0.3.tar.gz";
        this.containers.putResourceToArtipie(
            meta,
            String.format("/var/artipie/data/%s/artipietestpkg/artipietestpkg-0.0.3.tar.gz", repo)
        );
        this.containers.assertExec(
            "Failed to install package",
            new ContainerResultMatcher(
                Matchers.equalTo(0),
                new StringContainsInOrder(
                    new ListOf<>(
                        String.format("Looking in indexes: http://artipie:%s/%s", port, repo),
                        "Collecting artipietestpkg",
                        String.format(
                            "  Downloading http://artipie:%s/%s/artipietestpkg/%s",
                            port, repo, "artipietestpkg-0.0.3.tar.gz"
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
            String.format("http://artipie:%s/%s", port, repo),
            "artipietestpkg"
        );
    }

    @ParameterizedTest
    @CsvSource({
        "8080,my-python",
        "8081,my-python-port"
    })
    void canUpload(final String port, final String repo) throws Exception {
        this.containers.assertExec(
            "Failed to upload",
            new ContainerResultMatcher(
                Matchers.is(0),
                new StringContainsInOrder(
                    new ListOf<>(
                        "Uploading artipietestpkg-0.0.3.tar.gz", "100%"
                    )
                )
            ),
            "python3", "-m", "twine", "upload", "--repository-url",
            String.format("http://artipie:%s/%s/", port, repo),
            "-u", "alice", "-p", "123",
            "/var/artipie/data/artipie/pypi/example-pckg/dist/artipietestpkg-0.0.3.tar.gz"
        );
        this.containers.assertArtipieContent(
            "Bad content after upload",
            String.format("/var/artipie/data/%s/artipietestpkg/artipietestpkg-0.0.3.tar.gz", repo),
            Matchers.not("123".getBytes())
        );
    }
}
