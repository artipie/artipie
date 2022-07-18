/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;

/**
 * Integration test for Composer repo.
 * @since 0.18
 * @todo #1041:30min PhpComposerITCase: Add test cases with repository on individual port: create
 *  one more repository with `port` settings and start it in Artipie container exposing the port
 *  with `withExposedPorts` method. Then, parameterize test cases to check repositories with
 *  different ports. Check `FileITCase` as an example.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class PhpComposerITCase {
    /**
     * Package for installation.
     */
    static final String PACK = "log-1.1.4.zip";

    /**
     * Deployment for tests.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("composer/php.yml", "php"),
        () -> new TestDeployment.ClientContainer("composer:2.0.9")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                String.format("composer/%s", PhpComposerITCase.PACK),
                String.format("/w/%s", PhpComposerITCase.PACK),
                BindMode.READ_ONLY
            ).withClasspathResourceMapping(
                "composer/composer.json",
                "/w/repo/composer.json",
                BindMode.READ_ONLY
            )

    );

    @Test
    void canUploadAndInstall() throws IOException {
        final String url = "http://artipie:8080/php";
        this.containers.assertExec(
            "Failed to upload composer package archive",
            new ContainerResultMatcher(),
            "curl", "-X", "PUT", String.format("%s/%s", url, "log-1.1.4.zip"),
            "--upload-file", String.format("/w/%s", PhpComposerITCase.PACK),
            "--verbose"
        );
        this.containers.assertExec(
            "Failed to install uploaded package",
            new ContainerResultMatcher(),
            "env", "COMPOSER=/w/repo/composer.json",
            "composer", "install", "--verbose", "--no-cache"
        );
    }
}
