/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.file;

import com.artipie.maven.MavenITCase;
import com.artipie.test.TestDeployment;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration test for files proxy.
 *
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
final class FileProxyAuthIT {

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
                    .withRepoConfig("binary/bin-with-perms.yml", "my-bin")
                    .withCredentials("_credentials.yaml")
            ),
            new MapEntry<>(
                "artipie-proxy",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("binary/bin-proxy.yml", "my-bin-proxy")
            )
        ),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.containers.assertExec(
            "Failed to install deps",
            new MavenITCase.ContainerResultMatcher(),
            "apk", "add", "--no-cache", "curl"
        );
    }

    @Test
    void shouldGetAndCacheFile() throws Exception {
        final byte[] data = "Hello world!".getBytes();
        this.containers.putBinaryToArtipie(
            "artipie", data,
            "/var/artipie/data/my-bin/foo/bar.txt"
        );
        this.containers.assertExec(
            "File was not downloaded",
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0), new StringContains("HTTP/1.1 200 OK")
            ),
            "curl", "-i", "-X", "GET", "http://artipie-proxy:8080/my-bin-proxy/foo/bar.txt"
        );
    }
}
