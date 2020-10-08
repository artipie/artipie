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
package com.artipie.docker;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.docker.junit.DockerClient;
import com.artipie.docker.junit.DockerClientSupport;
import com.artipie.docker.proxy.ProxyDocker;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for {@link ProxyDocker}.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.10
 * @todo #499:30min Add integration test for Docker proxy cache feature.
 *  Docker proxy supports caching feature for it's remote repositories.
 *  Cache is populated when image is downloaded asynchronously
 *  and later used if remote repository is unavailable.
 *  This feature should be tested.
 * @todo #499:30min Add integration test for Docker proxy push feature.
 *  Docker proxy supports pushing to local storage if such storage is specified.
 *  It should be verified that an image can be pushed to proxy repository and pulled later.
 * @todo #449:30min Support running DockerProxyIT test on Windows.
 *  Running test on Windows uses `mcr.microsoft.com/dotnet/core/runtime` image.
 *  Loading this image manifest fails with
 *  "java.lang.IllegalStateException: multiple subscribers not supported" error.
 *  It seems that body is being read by some other entity in Artipie,
 *  so it requires investigation.
 *  Similar `CachingProxyITCase` tests works well in docker-adapter module.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs(OS.LINUX)
@DockerClientSupport
final class DockerProxyIT {

    /**
     * Example image to use in tests.
     */
    private Image image;

    /**
     * Docker client.
     */
    private DockerClient client;

    /**
     * Tested Artipie server.
     */
    private ArtipieServer server;

    /**
     * Docker repository.
     */
    private String repository;

    @BeforeEach
    void setUp(@TempDir final Path root) throws Exception {
        this.server = new ArtipieServer(
            root, "my-docker",
            new RepoConfigYaml("docker-proxy").withRemotes(
                Yaml.createYamlSequenceBuilder()
                    .add(
                        Yaml.createYamlMappingBuilder()
                            .add("url", "registry-1.docker.io")
                            .build()
                    )
                    .add(
                        Yaml.createYamlMappingBuilder()
                            .add("url", "mcr.microsoft.com")
                            .build()
                    )
            )
        );
        final int port = this.server.start();
        this.repository = String.format("localhost:%d", port);
        this.image = new Image.ForOs();
        final ArtipieServer.User user = ArtipieServer.ALICE;
        this.client.login(user.name(), user.password(), this.repository);
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
    }

    @Test
    void shouldPullRemote() throws Exception {
        final String img = new Image.From(
            this.repository,
            String.format("my-docker/%s", this.image.name()),
            this.image.digest(),
            this.image.layer()
        ).remoteByDigest();
        final String output = this.client.run("pull", img);
        MatcherAssert.assertThat(
            output,
            new StringContains(String.format("Status: Downloaded newer image for %s", img))
        );
    }
}
