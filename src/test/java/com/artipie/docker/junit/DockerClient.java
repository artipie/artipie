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
package com.artipie.docker.junit;

import com.google.common.collect.ImmutableList;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Docker client. Allows to run docker commands and returns cli output.
 *
 * @since 0.10
 */
public final class DockerClient {

    /**
     * Directory to store docker commands output logs.
     */
    private final Path dir;

    /**
     * Ctor.
     *
     * @param dir Directory to store docker commands output logs.
     */
    DockerClient(final Path dir) {
        this.dir = dir;
    }

    /**
     * Execute docker login command.
     *
     * @param username Username.
     * @param password Password.
     * @param repository Repository.
     * @throws IOException When reading stdout fails or it is impossible to start the process.
     * @throws InterruptedException When thread interrupted waiting for command to finish.
     */
    public void login(final String username, final String password, final String repository)
        throws IOException, InterruptedException {
        this.run(
            "login",
            "--username", username,
            "--password", password,
            repository
        );
    }

    /**
     * Execute docker command with args.
     *
     * @param args Arguments that will be passed to docker.
     * @return Command output.
     * @throws IOException When reading stdout fails or it is impossible to start the process.
     * @throws InterruptedException When thread interrupted waiting for command to finish.
     */
    public String run(final String... args) throws IOException, InterruptedException {
        final Path stdout = this.dir.resolve(
            String.format("%s-stdout.txt", UUID.randomUUID().toString())
        );
        final List<String> command = ImmutableList.<String>builder()
            .add("docker")
            .add(args)
            .build();
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        final int code = new ProcessBuilder()
            .directory(this.dir.toFile())
            .command(command)
            .redirectOutput(stdout.toFile())
            .redirectErrorStream(true)
            .start()
            .waitFor();
        final String log = new String(Files.readAllBytes(stdout));
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        if (code != 0) {
            throw new IllegalStateException(String.format("Not OK exit code: %d", code));
        }
        return log;
    }
}
