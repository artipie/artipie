/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.junit;

import com.google.common.collect.ImmutableList;
import com.jcabi.log.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Docker client. Allows to run docker commands and returns cli output.
 *
 * @since 0.3
 */
public final class DockerClient {
    /**
     * Directory to store docker commands output logs.
     */
    private final Path dir;

    /**
     * Ctor.
     * @param dir Directory to store docker commands output logs.
     */
    DockerClient(final Path dir) {
        this.dir = dir;
    }

    /**
     * Execute docker command with args.
     *
     * @param args Arguments that will be passed to docker
     * @return Output from docker
     * @throws Exception When something go wrong
     */
    public String run(final String... args) throws Exception {
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
