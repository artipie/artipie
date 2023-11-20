/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
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
        final Result result = this.runUnsafe(args);
        final int code = result.returnCode();
        if (code != 0) {
            throw new IllegalStateException(String.format("Not OK exit code: %d", code));
        }
        return result.output();
    }

    /**
     * Execute docker command with args.
     *
     * @param args Arguments that will be passed to docker.
     * @return Command result including return code and output.
     * @throws IOException When reading stdout fails or it is impossible to start the process.
     * @throws InterruptedException When thread interrupted waiting for command to finish.
     */
    public Result runUnsafe(final String... args) throws IOException, InterruptedException {
        final Path output = this.dir.resolve(
            String.format("%s-output.txt", UUID.randomUUID().toString())
        );
        final List<String> command = ImmutableList.<String>builder()
            .add("docker")
            .add(args)
            .build();
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        final int code = new ProcessBuilder()
            .directory(this.dir.toFile())
            .command(command)
            .redirectOutput(output.toFile())
            .redirectErrorStream(true)
            .start()
            .waitFor();
        final String log = new String(Files.readAllBytes(output));
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        return new Result(code, log);
    }

    /**
     * Docker client command execution result.
     *
     * @since 0.11
     */
    public static final class Result {

        /**
         * Return code.
         */
        private final int code;

        /**
         * Command output.
         */
        private final String out;

        /**
         * Ctor.
         *
         * @param code Return code.
         * @param out Command output.
         */
        public Result(final int code, final String out) {
            this.code = code;
            this.out = out;
        }

        /**
         * Read return code.
         *
         * @return Return code.
         */
        public int returnCode() {
            return this.code;
        }

        /**
         * Read command output.
         *
         * @return Command output string.
         */
        public String output() {
            return this.out;
        }
    }
}
