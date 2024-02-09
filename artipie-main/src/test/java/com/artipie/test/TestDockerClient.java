/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.test;

import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class TestDockerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDockerClient.class);

    /**
     * Insecure registry ports for a test docker client's demon.
     * If you are going to use a docker registry in the http mode
     * than you should start the registry on one of these ports.
     */
    public static final int[] INSECURE_PORTS = {52001, 52002, 52003};

    /**
     * Built from src/test/resources/docker/Dockerfile.
     */
    private static final DockerImageName DOCKER_CLIENT = DockerImageName.parse("dgarus/test-docker-client:1.0");

    private final int port;
    private final GenericContainer<?> client;

    public TestDockerClient(int port) {
        this.port = port;
        Testcontainers.exposeHostPorts(this.port);
        this.client = new GenericContainer<>(DOCKER_CLIENT)
                .withEnv("PORT", String.valueOf(port))
                .withPrivilegedMode(true)
                .withCommand("tail", "-f", "/dev/null");
    }

    public void start() throws IOException {
        this.client.start();
        execute("rc-service", "docker", "start");
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MICROSECONDS)
                .until(
                        () -> execute("docker", "info")
                                .map(res -> res.getExitCode() == 0)
                                .orElse(false)
                );
        LOGGER.debug("Client's docker daemon was started");
    }

    public void stop() {
        this.client.stop();
    }

    /**
     * Gets internal testcontainer host.
     *
     * @return Host
     */
    public String host() {
        return "host.testcontainers.internal:" + this.port;
    }

    public TestDockerClient login(String user, String pwd) throws IOException {
        return executeAssert("docker", "login", "--username", user,
                "--password", pwd, host());
    }

    public TestDockerClient pull(String image) throws IOException {
        return executeAssert(
                new StringContains("Status: Downloaded newer image for " + image),
                "docker", "pull", image
        );
    }

    public TestDockerClient push(String image) throws IOException {
        DockerImageName name = DockerImageName.parse(image);
        String expected = String.format(
                "The push refers to repository [%s]", name.getUnversionedPart()
        );
        return executeAssert(
                new StringContains(expected),
                "docker", "push", image
        );
    }

    public TestDockerClient remove(String image) throws IOException {
        return executeAssert(
                new StringContains("Untagged: " + image),
                "docker", "rmi", image
        );
    }

    public TestDockerClient tag(String source, String target) throws IOException {
        return executeAssert("docker", "tag", source, target);
    }

    public TestDockerClient executeAssert(String... cmd) throws IOException {
        Optional<Container.ExecResult> opt = execute(cmd);
        if (opt.isPresent()) {
            Container.ExecResult res = opt.get();
            Assertions.assertEquals(0, res.getExitCode(), res.getStderr());
        }
        return this;
    }

    public TestDockerClient executeAssert(Matcher<String> matcher, String... cmd) throws IOException {
        Optional<Container.ExecResult> opt = execute(cmd);
        if (opt.isPresent()) {
            Container.ExecResult res = opt.get();
            Assertions.assertEquals(0, res.getExitCode(), res.getStderr());
            MatcherAssert.assertThat(res.getStdout(), matcher);
        }
        return this;
    }

    public TestDockerClient executeAssertFail(String... cmd) throws IOException {
        Optional<Container.ExecResult> opt = execute(cmd);
        if (opt.isPresent()) {
            Container.ExecResult res = opt.get();
            Assertions.assertNotEquals(0, res.getExitCode(), res.getStdout());
        }
        return this;
    }

    public Optional<Container.ExecResult> executeAndLog(String... cmd) throws IOException {
        try {
            Container.ExecResult res = this.client.execInContainer(cmd);
            LOGGER.debug(
                    "Executed command: {}, exit_code={}\nstdout=[{}]\nstderr=[{}]",
                    Arrays.toString(cmd), res.getExitCode(), res.getStdout(), res.getStderr()
            );
            return Optional.of(this.client.execInContainer(cmd));
        } catch (final InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    private Optional<Container.ExecResult> execute(String... cmd) throws IOException {
        LOGGER.debug("Execute command: {}", Arrays.toString(cmd));
        try {
            return Optional.of(this.client.execInContainer(cmd));
        } catch (final InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }
}
