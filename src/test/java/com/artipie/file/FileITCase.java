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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration test for binary repo.
 * @since 0.18
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class FileITCase {

    /**
     * Deployment for tests.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("binary/bin.yml", "bin"),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.deployment.assertExec(
            "Failed to install deps",
            new MavenITCase.ContainerResultMatcher(Matchers.is(0)),
            "apk", "add", "--no-cache", "curl"
        );
    }

    @Test
    void canDownload() throws Exception {
        final byte[] target = new byte[]{0, 1, 2, 3};
        this.deployment.putBinaryToArtipie(target, "/var/artipie/data/bin/target");
        this.deployment.assertExec(
            "Failed to download artifact",
            new MavenITCase.ContainerResultMatcher(Matchers.is(0)),
            "curl", "-X", "GET", "http://artipie:8080/bin/target"
        );
    }

    @Test
    void canUpload() throws Exception {
        this.deployment.assertExec(
            "Failed to upload",
            new MavenITCase.ContainerResultMatcher(Matchers.is(0)),
            "curl", "-X", "PUT", "--data-binary", "123", "http://artipie:8080/bin/test"
        );
        this.deployment.assertArtipieContent(
            "Bad content after upload",
            "/var/artipie/data/bin/test",
            Matchers.equalTo("123".getBytes())
        );
    }
}
