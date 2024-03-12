/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.http.client.RemoteConfig;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.test.TestDockerClient;
import com.artipie.test.vertxmain.TestVertxMain;
import com.artipie.test.vertxmain.TestVertxMainBuilder;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Docker-proxy tests.
 */
public class DockerProxyPriorityIT {

    private static final String IMAGE = "alpine:3.19";

    @TempDir
    Path temp;
    private RemoteServers servers;
    private TestVertxMain proxy;
    private TestDockerClient bob;

    @BeforeEach
    void setUp() throws Exception {
        Path proxyWorkDir = Files.createDirectory(temp.resolve("proxy_instance"));
        servers = new RemoteServers(
            List.of(
                new RemoteServer(RandomFreePort.get(), 100),
                new RemoteServer(RandomFreePort.get(), 0),
                new RemoteServer(RandomFreePort.get(), -100),
                new RemoteServer(RandomFreePort.get(), 150),
                new RemoteServer(RandomFreePort.get(), 50)
            )
        );
        servers.start();
        proxy = new TestVertxMainBuilder(proxyWorkDir)
            .withUser("bob", "security/users/bob.yaml")
            .withDockerProxyRepo("proxy_repo", servers.remoteConfigs())
            .build(TestDockerClient.INSECURE_PORTS[0]);

        bob = new TestDockerClient(TestDockerClient.INSECURE_PORTS[0]);
        bob.start();
    }

    @AfterEach
    void tearDown() {
        bob.stop();
        proxy.close();
        servers.vertx.close();
    }

    @Test
    void shouldGetImage() throws Exception {
        final String proxyImage = bob.host() + "/proxy_repo/remote_repo/" + IMAGE;
        bob.login("bob", "qwerty").executeAssertFail("docker", "pull", proxyImage);

        Assertions.assertEquals(servers.remotes.size(), servers.queue.size());
        RemoteServer[] res = servers.queue.toArray(RemoteServer[]::new);
        for (int i = 1; i < res.length; i++) {
            Assertions.assertTrue(res[i - 1].priority() > res[i].priority());
        }
    }

    static class RemoteServers {
        final List<RemoteServer> remotes;
        private final Vertx vertx = Vertx.vertx();
        private final ConcurrentLinkedQueue<RemoteServer> queue = new ConcurrentLinkedQueue<>();

        public RemoteServers(List<RemoteServer> remotes) {
            this.remotes = remotes;
        }

        void start() {
            remotes.forEach(srv ->
                vertx.createHttpServer()
                    .requestHandler(new Handler<>() {
                        boolean first = true;

                        @Override
                        public void handle(HttpServerRequest req) {
                            if (first) {
                                queue.offer(srv);
                                first = false;
                            }
                            req.response().setStatusCode(404).end();
                        }
                    })
                    .listen(srv.port())
            );
        }

        RemoteConfig[] remoteConfigs() {
            return remotes
                .stream()
                .map(s -> new RemoteConfig(
                    URI.create("http://localhost:" + s.port), s.priority, null, null)
                ).toArray(RemoteConfig[]::new);
        }
    }

    record RemoteServer(int port, int priority) {
    }
}
