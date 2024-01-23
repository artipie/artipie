/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.util.Map;
import org.cactoos.map.MapEntry;
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
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
final class MavenProxyAuthIT {

    /**
     * Test deployments.
         */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        Map.ofEntries(
            new MapEntry<>(
                "artipie",
                () -> new TestDeployment.ArtipieContainer().withConfig("artipie_with_policy.yaml")
                    .withRepoConfig("maven/maven-with-perms.yml", "my-maven")
                    .withUser("security/users/alice.yaml", "alice")
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
            new ContainerResultMatcher(
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
