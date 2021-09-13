/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;

/**
 * Conda IT case.
 * @since 0.23
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class CondaITCase {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("conda/conda.yml", "my-conda"),
        () -> new TestDeployment.ClientContainer("continuumio/miniconda3:4.10.3")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "conda/condarc", "/w/.condarc", BindMode.READ_ONLY
            ).withClasspathResourceMapping(
                "conda/example-project", "/w/example-project", BindMode.READ_ONLY
            )
    );

    @BeforeEach
    void init() throws IOException {
        this.containers.assertExec(
            "Conda-build install failed", new ContainerResultMatcher(),
            "conda", "install", "-y", "conda-build"
        );
        this.containers.assertExec(
            "Conda-verify install failed", new ContainerResultMatcher(),
            "conda", "install", "-y", "conda-verify"
        );
        this.containers.assertExec(
            "Conda-client install failed", new ContainerResultMatcher(),
            "conda", "install", "-y", "anaconda-client"
        );
    }

    @Test
    void canInstallFromArtipie() throws IOException {
        this.moveCondarc();
        this.containers.putBinaryToArtipie(
            new TestResource("conda/packages.json").asBytes(),
            "/var/artipie/data/my-conda/linux-64/repodata.json"
        );
        this.containers.putBinaryToArtipie(
            new TestResource("conda/snappy-1.1.3-0.tar.bz2").asBytes(),
            "/var/artipie/data/my-conda/linux-64/snappy-1.1.3-0.tar.bz2"
        );
        this.containers.assertExec(
            "Package snappy-1.1.3-0 was not installed successfully",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                Matchers.allOf(
                    new StringContains("http://artipie:8080/my-conda"),
                    new StringContains("linux-64::snappy-1.1.3-0")
                )
            ),
            "conda", "install", "--verbose", "-y", "snappy"
        );
    }

    @Test
    void canUploadToArtipie() throws IOException {
        this.moveCondarc();
        this.containers.assertExec(
            "Failed to set anaconda upload url",
            new ContainerResultMatcher(),
            "anaconda", "config", "--set", "url", "http://artipie:8080/my-conda/", "-s"
        );
        this.containers.assertExec(
            "Failed to set anaconda upload flag",
            new ContainerResultMatcher(),
            "conda", "config", "--set", "anaconda_upload", "yes"
        );
        this.containers.assertExec(
            "Login was not successful",
            new ContainerResultMatcher(),
            "anaconda", "login", "--username", "any", "--password", "any"
        );
        this.containers.assertExec(
            "Package was not installed successfully",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                Matchers.allOf(
                    new StringContains("Using Anaconda API: http://artipie:8080/my-conda/"),
                    // @checkstyle LineLengthCheck (1 line)
                    new StringContains("Uploading file \"any/example-package/0.0.1/linux-64/example-package-0.0.1-0.tar.bz2\""),
                    new StringContains("Upload complete")
                )
            ),
            "conda", "build", "--output-folder", "/w/conda-out/", "/w/example-project/conda/"
        );
        this.containers.assertArtipieContent(
            "Package was not uploaded to artipie",
            "/var/artipie/data/my-conda/linux-64/example-package-0.0.1-0.tar.bz2",
            new IsNot<>(new IsNull<>())
        );
        this.containers.assertArtipieContent(
            "Package was not uploaded to artipie",
            "/var/artipie/data/my-conda/linux-64/repodata.json",
            new IsNot<>(new IsNull<>())
        );
    }

    private void moveCondarc() throws IOException {
        this.containers.assertExec(
            "Failed to move condarc to /root", new ContainerResultMatcher(),
            "mv", "/w/.condarc", "/root/"
        );
    }
}
