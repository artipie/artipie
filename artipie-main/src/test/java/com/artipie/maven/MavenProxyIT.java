/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import org.hamcrest.core.IsAnything;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Integration test for {@link com.artipie.maven.http.MavenProxySlice}.
 *
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
final class MavenProxyIT {

    /**
     * Test deployments.
             */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("maven/maven-proxy.yml", "my-maven")
            .withRepoConfig("maven/maven-proxy-port.yml", "my-maven-port")
            .withExposedPorts(8081),
        () -> new TestDeployment.ClientContainer("maven:3.6.3-jdk-11")
            .withWorkingDirectory("/w")
    );

    @ParameterizedTest
    @CsvSource({
        "my-maven,maven/maven-settings.xml",
        "my-maven-port,maven/maven-settings-port.xml"
    })
    void shouldGetArtifactFromCentralAndSaveInCache(final String repo,
        final String settings) throws Exception {
        this.containers.putClasspathResourceToClient(settings, "/w/settings.xml");
        this.containers.assertExec(
            "Artifact wasn't downloaded",
            new ContainerResultMatcher(
                new IsEqual<>(0), new StringContains("BUILD SUCCESS")
            ),
            "mvn", "-s", "settings.xml", "dependency:get", "-Dartifact=args4j:args4j:2.32:jar"
        );
        this.containers.assertArtipieContent(
            "Artifact wasn't saved in cache",
            String.format("/var/artipie/data/%s/args4j/args4j/2.32/args4j-2.32.jar", repo),
            new IsAnything<>()
        );
    }

}
