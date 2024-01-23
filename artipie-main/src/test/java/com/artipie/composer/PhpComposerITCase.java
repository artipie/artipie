/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.BindMode;

/**
 * Integration test for Composer repo.
 * @since 0.18
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class PhpComposerITCase {
    /**
     * Package for installation.
     */
    static final String PACK = "log-1.1.4.zip";

    /**
     * Deployment for tests.
         */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("composer/php.yml", "php")
            .withRepoConfig("composer/php-port.yml", "php-port")
            .withExposedPorts(8081),
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
            ).withClasspathResourceMapping(
                "composer/composer-port.json",
                "/w/repo/composer-port.json",
                BindMode.READ_ONLY
            )
    );

    @ParameterizedTest
    @CsvSource({
        "http://artipie:8080/php,composer.json",
        "http://artipie:8081/php-port,composer-port.json"
    })
    void canUploadAndInstall(final String url, final String stn) throws IOException {
        this.containers.assertExec(
            "Failed to upload composer package archive",
            new ContainerResultMatcher(),
            "curl", "-X", "PUT", String.format("%s/%s", url, PhpComposerITCase.PACK),
            "--upload-file", String.format("/w/%s", PhpComposerITCase.PACK),
            "--verbose"
        );
        this.containers.assertExec(
            "Failed to install uploaded package",
            new ContainerResultMatcher(),
            "env", String.format("COMPOSER=/w/repo/%s", stn),
            "composer", "install", "--verbose", "--no-cache"
        );
    }
}
