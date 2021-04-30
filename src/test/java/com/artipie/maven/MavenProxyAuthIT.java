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

import com.artipie.asto.test.TestResource;
import com.artipie.test.TestDeployment;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
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
final class MavenProxyAuthIT {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        new MapOf<>(
            new MapEntry<>(
                "artipie",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("maven/maven-with-perms.yml", "my-maven")
                    .withCredentials("_credentials.yaml")
            ),
            new MapEntry<>(
                "artipie-proxy",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("maven/maven-proxy-artipie.yml", "my-maven-proxy")
            )
        ),
        () -> new TestDeployment.ClientContainer("maven:3.6.3-jdk-11")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "maven/maven-settings-proxy.xml", "/w/settings.xml", BindMode.READ_ONLY
            )
    );

    @Test
    void shouldGetDependency() throws Exception {
        this.containers.putResourceToArtipie(
            "artipie",
            "com/artipie/helloworld/maven-metadata.xml",
            "/var/artipie/data/my-maven/com/artipie/helloworld/maven-metadata.xml"
        );
        MavenITCase.getResourceFiles("com/artipie/helloworld/0.1")
            .stream().map(item -> String.join("/", "com/artipie/helloworld/0.1", item))
            .forEach(
                item -> this.containers.putResourceToArtipie(
                    item, String.join("/", "/var/artipie/data/my-maven", item)
                )
            );
        this.containers.assertExec(
            "Helloworld was not installed",
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContains("BUILD SUCCESS")
            ),
            "mvn", "-s", "settings.xml",
            "dependency:get", "-Dartifact=com.artipie:helloworld:0.1:jar"
        );
        this.containers.assertArtipieContent(
            "artipie-proxy",
            "Artifact was not cached in proxy",
            "/var/artipie/data/my-maven-proxy/com/artipie/helloworld/0.1/helloworld-0.1.jar",
            new IsEqual<>(
                new TestResource("com/artipie/helloworld/0.1/helloworld-0.1.jar").asBytes()
            )
        );
    }

}
