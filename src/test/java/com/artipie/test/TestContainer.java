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
        this(
            new GenericContainer<>(image)
                .withCommand("tail", "-f", "/dev/null")
                .withWorkingDirectory("/home/")
                .withFileSystemBind(path.toString(), "/home")
        );
    }

    /**
     * Ctor.
     * @param cntn Generic container
     */
    public TestContainer(final GenericContainer<?> cntn) {
        this.cntn = cntn;
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
        final Container.ExecResult exec = this.exec(command);
        this.logStd(exec, true);
        return exec.getStderr().replace("\n", "");
    }

    /**
     * Stdout result of the command execution.
     * @param command Command for execution in container
     * @return Stdout result of the execution.
     * @throws Exception In case of exception during execution command in container.
     */
    public String execStdout(final String... command) throws Exception {
        final Container.ExecResult exec = this.exec(command);
        final int code = exec.getExitCode();
        if (code != 0) {
            this.logStd(exec, true);
            throw new IllegalStateException(
                String.format(
                    "'%s' failed with %s code", String.join(" ", command), code
                )
            );
        }
        this.logStd(exec, false);
        return exec.getStdout().replace("\n", "");
    }

    /**
     * Stdout result of the command execution without checking exit code.
     * It is necessary to verify that output contains required information
     * in case exit code is not equal 0.
     * @param command Command for execution in container
     * @return Stdout result of the execution.
     * @throws Exception In case of exception during execution command in container.
     */
    public String execStdoutWithoutCheckExitCode(final String... command) throws Exception {
        final Container.ExecResult exec = this.exec(command);
        this.logStd(exec, true);
        return exec.getStdout().replace("\n", "");
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

    /**
     * Log std result of the command execution.
     * @param exec Result of the execution
     * @param error Is it necessary to log stderr as error?
     */
    private void logStd(final Container.ExecResult exec, final boolean error) {
        final String outputerr = String.format("\nSTDERR:\n%s", exec.getStderr());
        if (error) {
            Logger.info(this, String.format("\nSTDOUT:\n%s", exec.getStdout()));
            Logger.error(this, outputerr);
        } else {
            Logger.warn(this, outputerr);
        }
    }

}
