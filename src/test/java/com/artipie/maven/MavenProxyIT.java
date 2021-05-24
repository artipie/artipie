/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.test.TestDeployment;
import org.hamcrest.core.IsAnything;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;

/**
 * Integration test for {@link com.artipie.maven.http.MavenProxySlice}.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
final class MavenProxyIT {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("maven/maven-proxy.yml", "my-maven"),
        () -> new TestDeployment.ClientContainer("maven:3.6.3-jdk-11")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "maven/maven-settings.xml", "/w/settings.xml", BindMode.READ_ONLY
            )
    );

    @Test
    void shouldGetArtifactFromCentralAndSaveInCache() throws Exception {
        this.containers.assertExec(
            "Artifact wasn't downloaded",
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0), new StringContains("BUILD SUCCESS")
            ),
            "mvn", "-s", "settings.xml", "dependency:get", "-Dartifact=args4j:args4j:2.32:jar"
        );
        this.containers.assertArtipieContent(
            "Artifact wasn't saved in cache",
            "/var/artipie/data/my-maven/args4j/args4j/2.32/args4j-2.32.jar",
            new IsAnything<>()
        );
    }

}
