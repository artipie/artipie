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
package com.artipie.pypi;

import com.artipie.asto.test.TestResource;
import com.artipie.maven.MavenITCase;
import com.artipie.test.TestDeployment;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test to pypi proxy.
 * @since 0.12
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
@Disabled
public final class PypiProxyITCase {

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
                    .withRepoConfig("pypi-proxy/pypi.yml", "my-pypi")
                    .withCredentials("pypi-proxy/_credentials.yaml")
            ),
            new MapEntry<>(
                "artipie-proxy",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withRepoConfig("pypi-proxy/pypi-proxy.yml", "my-pypi-proxy")
                    .withCredentials("pypi-proxy/_credentials.yaml")
            )
        ),
        () -> new TestDeployment.ClientContainer("python:3")
            .withWorkingDirectory("/w")
    );

    @Test
    void installFromProxy() throws Exception {
        final byte[] data = new TestResource("pypi-repo/alarmtime-0.1.5.tar.gz").asBytes();
        this.containers.putBinaryToArtipie(
            "artipie", data,
            "/var/artipie/data/my-pypi/alarmtime/alarmtime-0.1.5.tar.gz"
        );
        this.containers.assertExec(
            "Package was not installed",
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0),
                Matchers.containsString("Successfully installed alarmtime-0.1.5")
            ),
            "pip", "install", "--no-deps", "--trusted-host", "artipie-proxy",
            "--index-url", "http://alice:123@artipie-proxy:8080/my-pypi-proxy/", "alarmtime"
        );
        this.containers.assertArtipieContent(
            "artipie-proxy",
            "/var/artipie/data/my-pypi-proxy/alarmtime/alarmtime-0.1.5.tar.gz",
            new IsEqual<>(data)
        );
    }

}
