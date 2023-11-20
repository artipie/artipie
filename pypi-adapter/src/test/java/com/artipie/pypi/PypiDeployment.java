/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi;

import com.artipie.http.misc.RandomFreePort;
import com.jcabi.log.Logger;
import java.io.IOException;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/**
 * A class with base utility  for tests, that instantiates container with python runtime.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
public final class PypiDeployment implements BeforeEachCallback, AfterEachCallback {

    /**
     * Python container.
     */
    private final PypiContainer container = new PypiContainer();

    /**
     * Port.
     */
    private final int prt = new RandomFreePort().get();

    /**
     * Executes a bash command in a python container.
     *
     * @param command Bash command to execute.
     * @return Stdout of command.
     * @throws IOException          If fails.
     * @throws InterruptedException If fails.
     */
    public String bash(final String command) throws IOException, InterruptedException {
        final Container.ExecResult exec =  this.container.execInContainer(
            "/bin/bash", "-c", command
        );
        Logger.info(this, exec.getStdout());
        Logger.info(this, exec.getStderr());
        return exec.getStdout();
    }

    /**
     * Address to access local port from the docker container.
     * @return Address
     */
    public String localAddress() {
        return String.format("http://host.testcontainers.internal:%d/", this.prt);
    }

    /**
     * Address to access local port from the docker container.
     * @param user Username
     * @param pswd Password
     * @return Address
     */
    public String localAddress(final String user, final String pswd) {
        return
            String.format("http://%s:%s@host.testcontainers.internal:%d/", user, pswd, this.prt);
    }

    /**
     * Put binary data into container /home/ directory.
     * @param bin Data to put
     * @param path Path in the container
     */
    public void putBinaryToContainer(final byte[] bin, final String path) {
        this.container.copyFileToContainer(
            Transferable.of(bin), String.format("./home/%s", path)
        );
    }

    @Override
    public void beforeEach(final ExtensionContext extension) throws Exception {
        Testcontainers.exposeHostPorts(this.prt);
        this.container
            .withCommand("tail", "-f", "/dev/null")
            .setWorkingDirectory("/home/");
        this.container.start();
        this.bash("python3 -m pip install --user --upgrade twine");
    }

    @Override
    public void afterEach(final ExtensionContext extension) throws Exception {
        this.container.stop();
    }

    /**
     * Port, exposed to testcontainers.
     * @return Port value
     */
    public int port() {
        return this.prt;
    }

    /**
     * Client container builder.
     * @since 0.19
     */
    public static final class PypiContainer extends GenericContainer<PypiContainer> {

        /**
         * New client container with name.
         */
        public PypiContainer() {
            super(DockerImageName.parse("python:3.7"));
        }
    }
}
