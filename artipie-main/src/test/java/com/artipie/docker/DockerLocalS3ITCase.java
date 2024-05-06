/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.test.TestDeployment;
import com.artipie.test.TestDockerClient;
import com.artipie.test.vertxmain.TestVertxMain;
import com.artipie.test.vertxmain.TestVertxMainBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.nio.file.Path;

/**
 * Integration test for local Docker repositories with S3 storage.
 */
final class DockerLocalS3ITCase {

    /**
     * MinIO S3 storage server port.
     */
    private static final int S3_PORT = 9000;

    @TempDir
    Path temp;

    private TestVertxMain server;

    private TestDockerClient client;
    private Storage storage;

    @BeforeEach
    void setUp() throws Exception {
        client = new TestS3DockerClient(TestDockerClient.INSECURE_PORTS[0]);
        client.start();
        this.client.executeAssert(
            "sh", "-c", "nohup /root/bin/minio server /var/minio 2>&1|tee /tmp/minio.log &"
        );
        this.client.executeAssert(
            "timeout", "30",  "sh", "-c", "until nc -z localhost 9000; do sleep 0.1; done"
        );
        final int s3port = this.client.getMappedPort(S3_PORT);
        final YamlMapping repo = Yaml.createYamlMappingBuilder()
            .add("type", "s3")
            .add("region", "s3test")
            .add("bucket", "buck1")
            .add("endpoint", String.format("http://localhost:%d", s3port))
            .add(
                "credentials",
                Yaml.createYamlMappingBuilder()
                    .add("type", "basic")
                    .add("accessKeyId", "minioadmin")
                    .add("secretAccessKey", "minioadmin")
                    .build()
            )
            .build();
        this.storage = StoragesLoader.STORAGES
            .newObject("s3", new Config.YamlStorageConfig(repo));
        server = new TestVertxMainBuilder(temp)
            .withUser("alice", "security/users/alice.yaml")
            .withDockerRepo("registry", repo)
            .build(TestDockerClient.INSECURE_PORTS[0]);
    }

    @AfterEach
    void tearDown() {
        client.stop();
        server.close();
    }

    @Test
    void pushAndPull() throws Exception {
        MatcherAssert.assertThat(
            "Repository storage must be empty before test",
            storage.list(Key.ROOT).join().isEmpty()
        );
        final String image = client.host() + "/registry/alpine:3.19.1";
        client.login("alice", "123")
            .pull("alpine:3.19.1")
            .tag("alpine:3.19.1", image)
            .push(image)
            .remove(image)
            .pull(image);
        MatcherAssert.assertThat(
            "Repository storage must not be empty after test",
            !storage.list(Key.ROOT).join().isEmpty()
        );
    }

    private static final class TestS3DockerClient extends TestDockerClient {
        public TestS3DockerClient(int port) {
            super(port, TestS3DockerClient.prepareClientContainer(port));
        }

        private static TestDeployment.ClientContainer prepareClientContainer(int port) {
            return new TestDeployment.ClientContainer(TestDockerClient.DOCKER_CLIENT.toString())
                .withEnv("PORT", String.valueOf(port))
                .withPrivilegedMode(true)
                .withCommand("tail", "-f", "/dev/null")
                .withAccessToHost(true)
                .withWorkingDirectory("/w")
                .withNetworkAliases("minic")
                .withExposedPorts(DockerLocalS3ITCase.S3_PORT)
                .waitingFor(
                    new AbstractWaitStrategy() {
                        @Override
                        protected void waitUntilReady() {
                            // Don't wait for minIO S3 port.
                        }
                    }
                );
        }
    }
}
