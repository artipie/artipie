/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.test.TestDockerClient;
import com.artipie.test.vertxmain.TestVertxMain;
import com.artipie.test.vertxmain.TestVertxMainBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * Integration test for local Docker repository running on port.
 */
final class DockerOnPortIT {

    @TempDir
    Path temp;

    /**
     * Repository port.
     */
    private static final int PORT = TestDockerClient.INSECURE_PORTS[0];

    /**
     * Example docker image to use in tests.
     */
    private Image image;

    private TestVertxMain server;

    private TestDockerClient client;

    @BeforeEach
    void setUp() throws Exception {
        this.server = new TestVertxMainBuilder(temp)
                .withUser("alice", "security/users/alice.yaml")
                .withDockerRepo("my-docker", DockerOnPortIT.PORT, temp.resolve("data"))
                .build();
        client = new TestDockerClient(DockerOnPortIT.PORT);
        client.start();
        image = this.prepareImage();
        client.login("alice", "123");

    }

    @AfterEach
    void tearDown() {
        client.stop();
        server.close();
    }

    @Test
    void shouldPush() throws Exception {
        client.push(this.image.remote());
    }

    @Test
    void shouldPullPushed() throws Exception {
        client.push(this.image.remote());
        client.executeAssert("docker", "image", "rm", image.name());
        client.executeAssert("docker", "image", "rm", image.remote());
        client.pull(this.image.remote());
    }

    private Image prepareImage() throws Exception {
        final Image source = new Image.ForOs();
        client.pull(source.remoteByDigest());
        client.tag(source.remoteByDigest(), "my-test:latest");
        final Image img = new Image.From(
                client.host(),
                "my-test",
                source.digest(),
                source.layer()
        );
        client.tag(source.remoteByDigest(), img.remote());
        return img;
    }
}
