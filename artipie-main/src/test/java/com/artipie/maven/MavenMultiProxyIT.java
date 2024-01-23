/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Integration test for maven proxy with multiple remotes.
 *
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
final class MavenMultiProxyIT {

    /**
     * Test deployments.
             */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        new MapOf<>(
            new MapEntry<>(
                "artipie",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("maven/maven-multi-proxy.yml", "my-maven")
                    .withRepoConfig("maven/maven-multi-proxy-port.yml", "my-maven-port")
                    .withExposedPorts(8081)
            ),
            new MapEntry<>(
                "artipie-empty",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("maven/maven.yml", "empty-maven")
            ),
            new MapEntry<>(
                "artipie-origin",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("maven/maven.yml", "origin-maven")
            )
        ),
        () -> new TestDeployment.ClientContainer("maven:3.6.3-jdk-11")
            .withWorkingDirectory("/w")
    );

    @ParameterizedTest
    @ValueSource(strings = {
        "maven/maven-settings.xml",
        "maven/maven-settings-port.xml"
    })
    void shouldGetDependency(final String settings) throws Exception {
        this.containers.putClasspathResourceToClient(settings, "/w/settings.xml");
        this.containers.putResourceToArtipie(
            "artipie-origin",
            "com/artipie/helloworld/maven-metadata.xml",
            "/var/artipie/data/origin-maven/com/artipie/helloworld/maven-metadata.xml"
        );
        MavenITCase.getResourceFiles("com/artipie/helloworld/0.1")
            .stream().map(item -> String.join("/", "com/artipie/helloworld/0.1", item))
            .forEach(
                item -> this.containers.putResourceToArtipie(
                    "artipie-origin", item, String.join("/", "/var/artipie/data/origin-maven", item)
                )
            );
        this.containers.assertExec(
            "Artifact wasn't downloaded",
            new ContainerResultMatcher(
                new IsEqual<>(0), new StringContains("BUILD SUCCESS")
            ),
            "mvn", "-s", "settings.xml", "dependency:get",
            "-Dartifact=com.artipie:helloworld:0.1:jar"
        );
    }

}
