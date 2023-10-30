/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.file;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import java.util.Map;
import org.cactoos.map.MapEntry;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
     * @checkstyle MagicNumberCheck (20 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        Map.ofEntries(
            new MapEntry<>(
                "artipie",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("binary/bin.yml", "my-bin")
                    .withUser("security/users/alice.yaml", "alice")
            ),
            new MapEntry<>(
                "artipie-proxy",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("binary/bin-proxy.yml", "my-bin-proxy")
                    .withRepoConfig("binary/bin-proxy-cache.yml", "my-bin-proxy-cache")
                    .withRepoConfig("binary/bin-proxy-port.yml", "my-bin-proxy-port")
                    .withExposedPorts(8081)
            )
        ),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.containers.assertExec(
            "Failed to install deps",
            new ContainerResultMatcher(),
            "apk", "add", "--no-cache", "curl"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"8080/my-bin-proxy", "8081/my-bin-proxy-port"})
    void shouldGetFileFromOrigin(final String repo) throws Exception {
        final byte[] data = "Hello world!".getBytes();
        this.containers.putBinaryToArtipie(
            "artipie", data,
            "/var/artipie/data/my-bin/foo/bar.txt"
        );
        this.containers.assertExec(
            "File was not downloaded",
            new ContainerResultMatcher(
                new IsEqual<>(0), new StringContains("HTTP/1.1 200 OK")
            ),
            "curl", "-i", "-X", "GET", String.format("http://artipie-proxy:%s/foo/bar.txt", repo)
        );
    }

    @Test
    void cachesDataWhenCacheIsSet() throws IOException {
        final byte[] data = "Hello world!".getBytes();
        this.containers.putBinaryToArtipie(
            "artipie", data,
            "/var/artipie/data/my-bin/foo/bar.txt"
        );
        this.containers.assertExec(
            "File was not downloaded",
            new ContainerResultMatcher(
                new IsEqual<>(0), new StringContains("HTTP/1.1 200 OK")
            ),
            "curl", "-i", "-X", "GET", "http://artipie-proxy:8080/my-bin-proxy-cache/foo/bar.txt"
        );
        this.containers.assertArtipieContent(
            "artipie-proxy", "Proxy cached data",
            "/var/artipie/data/my-bin-proxy-cache/foo/bar.txt",
            new IsEqual<>(data)
        );
    }
}
