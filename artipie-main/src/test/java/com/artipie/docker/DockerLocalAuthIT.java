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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Integration test for auth in local Docker repositories.
 */
final class DockerLocalAuthIT {

    @TempDir
    Path temp;

    private TestVertxMain server;

    private TestDockerClient client;

    private String image;

    @BeforeEach
    void setUp() throws Exception {
        server = new TestVertxMainBuilder(temp)
                .withUser("alice", "security/users/alice.yaml")
                .withUser("bob", "security/users/bob.yaml")
                .withRole("readers", "security/roles/readers.yaml")
                .withDockerRepo("registry", temp.resolve("data"))
                .build(TestDockerClient.INSECURE_PORTS[0]);
        client = new TestDockerClient(server.port());
        client.start();

        image = client.host() + "/registry/alpine:3.11";
    }

    @AfterEach
    void tearDown() {
        client.stop();
        server.close();
    }

    @Test
    void aliceCanPullAndPush() throws IOException {
        client.login("alice", "123")
                .pull("alpine:3.11")
                .tag("alpine:3.11", image)
                .push(image)
                .remove(image)
                .pull(image);
    }

    @Test
    void canPullWithReadPermission() throws IOException {
        client.login("alice", "123")
                .pull("alpine:3.11")
                .tag("alpine:3.11", image)
                .push(image)
                .remove(image)
                .executeAssert("docker", "logout", client.host())
                .login("bob", "qwerty")
                .pull(image);
    }

    @Test
    void shouldFailPushIfNoWritePermission() throws Exception {
        client.login("bob", "qwerty")
                .pull("alpine:3.11")
                .tag("alpine:3.11", image)
                .executeAssertFail("timeout", "20s", "docker", "push", image);
    }

    @Test
    void shouldFailPushIfAnonymous() throws IOException {
        client.pull("alpine:3.11")
                .tag("alpine:3.11", image)
                .executeAssertFail("docker", "push", image);
    }

    @Test
    void shouldFailPullIfAnonymous() throws IOException {
        client.login("alice", "123")
                .pull("alpine:3.11")
                .tag("alpine:3.11", image)
                .push(image)
                .remove(image)
                .executeAssert("docker", "logout", client.host())
                .executeAssertFail("docker", "pull", image);
    }
}
