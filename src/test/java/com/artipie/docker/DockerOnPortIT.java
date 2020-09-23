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
import com.artipie.docker.junit.DockerClient;
import com.artipie.docker.junit.DockerClientSupport;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for local Docker repository running on port.
 *
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DockerClientSupport
final class DockerOnPortIT {

    /**
     * Example docker image to use in tests.
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
        final int port = freePort();
        final String config = Yaml.createYamlMappingBuilder().add(
            "repo",
            Yaml.createYamlMappingBuilder()
                .add("type", "docker")
                .add(
                    "storage",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "fs")
                        .add("path", root.resolve("data").toString())
                        .build()
                )
                .add("port", String.valueOf(port))
                .build()
        ).build().toString();
        this.server = new ArtipieServer(root, "my-docker", config);
        this.server.start();
        this.repository = String.format("localhost:%d", port);
        this.image = this.prepareImage();
        final ArtipieServer.User user = ArtipieServer.ALICE;
        this.client.login(user.name(), user.password(), this.repository);
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
    }

    @Test
    void shouldPush() throws Exception {
        final String output = this.client.run("push", this.image.remote());
        MatcherAssert.assertThat(
            output,
            new AllOf<>(
                Arrays.asList(
                    new StringContains(String.format("%s: Pushed", this.image.layer())),
                    new StringContains(String.format("latest: digest: %s", this.image.digest()))
                )
            )
        );
    }

    @Test
    void shouldPullPushed() throws Exception {
        this.client.run("push", this.image.remote());
        this.client.run("image", "rm", this.image.name());
        this.client.run("image", "rm", this.image.remote());
        final String output = this.client.run("pull", this.image.remote());
        MatcherAssert.assertThat(
            output,
            new StringContains(
                String.format("Status: Downloaded newer image for %s", this.image.remote())
            )
        );
    }

    private Image prepareImage() throws Exception {
        final Image source = new Image.ForOs();
        this.client.run("pull", source.remoteByDigest());
        final String local = "my-docker/my-test";
        this.client.run("tag", source.remoteByDigest(), String.format("%s:latest", local));
        final Image img = new Image.From(
            this.repository,
            local,
            source.digest(),
            source.layer()
        );
        this.client.run("tag", source.remoteByDigest(), img.remote());
        return img;
    }

    /**
     * Obtain free port.
     *
     * @return Free port.
     */
    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
