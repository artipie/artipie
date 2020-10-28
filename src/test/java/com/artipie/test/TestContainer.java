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
package com.artipie.test;

import com.jcabi.log.Logger;
import java.nio.file.Path;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * Class for creating test container and for executing queries in it.
 * @since 0.12
 */
public final class TestContainer implements AutoCloseable {

    /**
     * Container.
     */
    private final GenericContainer<?> cntn;

    /**
     * Ctor.
     * @param image Docker image
     * @param path Path for binding file system
     */
    public TestContainer(final String image, final Path path) {
        this.cntn = new GenericContainer<>(image)
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(path.toString(), "/home");
    }

    /**
     * Start container exposing specified port.
     * @param port Port for exposing
     */
    public void start(final int port) {
        Testcontainers.exposeHostPorts(port);
        this.cntn.start();
    }

    /**
     * Stderr result of the command execution.
     * @param command Command for execution in container
     * @return Stderr result of the execution.
     * @throws Exception In case of exception during execution command in container.
     */
    public String execStdErr(final String... command) throws Exception {
        return this.exec(command).getStderr().replace("\n", "");
    }

    /**
     * Stdout result of the command execution.
     * @param command Command for execution in container
     * @return Stdout result of the execution.
     * @throws Exception In case of exception during execution command in container.
     */
    public String execStdout(final String... command) throws Exception {
        return this.exec(command).getStdout().replace("\n", "");
    }

    @Override
    public void close() {
        this.cntn.stop();
    }

    /**
     * Result of the command execution.
     * @param command Command for execution in container
     * @return Result of the execution.
     * @throws Exception In case of exception during execution command in container.
     */
    private Container.ExecResult exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command);
    }
}
