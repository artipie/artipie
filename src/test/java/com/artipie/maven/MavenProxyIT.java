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
