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
