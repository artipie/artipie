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
 * Integration test for local Docker repositories.
 */
final class DockerLocalITCase {

    @TempDir
    Path temp;

    private TestVertxMain server;

    private TestDockerClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new TestVertxMainBuilder(temp)
                .withUser("alice", "security/users/alice.yaml")
                .withDockerRepo("registry", temp.resolve("data"))
                .build(TestDockerClient.INSECURE_PORTS[0]);
        client = new TestDockerClient(server.port());
        client.start();
    }

    @AfterEach
    void tearDown() {
        client.stop();
        server.stop();
    }

    @Test
    void pushAndPull() throws Exception {
        final String image = client.host() + "/registry/alpine:3.11";
        client.login("alice", "123")
            .pull("alpine:3.11")
            .tag("alpine:3.11", image)
            .push(image)
            .remove(image)
            .pull(image);
    }
}
