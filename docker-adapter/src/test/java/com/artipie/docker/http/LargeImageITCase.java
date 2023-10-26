/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.junit.DockerClient;
import com.artipie.docker.junit.DockerClientSupport;
import com.artipie.docker.junit.DockerRepository;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for large file pushing scenario of {@link DockerSlice}.
 *
 * @since 0.3
*/
@DockerClientSupport
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class LargeImageITCase {
    /**
     * Docker image name.
     */
    private static final String IMAGE = "large-image";

    /**
     * Docker client.
     */
    private DockerClient client;

    /**
     * Docker repository.
     */
    private DockerRepository repository;

    @BeforeEach
    void setUp(final @TempDir Path storage) {
        this.repository = new DockerRepository(
            new AstoDocker(new FileStorage(storage))
        );
        this.repository.start();
    }

    @AfterEach
    void tearDown() {
        this.repository.stop();
    }

    @Test
    void largeImagePullWorks() throws Exception {
        try {
            this.buildImage();
            this.client.run("push", this.remote());
            this.client.run("image", "rm", this.remote());
            final String output = this.client.run("pull", this.remote());
            MatcherAssert.assertThat(
                output,
                new StringContains(
                    false,
                    String.format("Status: Downloaded newer image for %s", this.remote())
                )
            );
        } finally {
            this.client.run("rmi", this.remote());
        }
    }

    @Test
    void largeImageUploadWorks() throws Exception {
        try {
            this.buildImage();
            final String output = this.client.run("push", this.remote());
            MatcherAssert.assertThat(output, new StringContains(false, "Pushed"));
        } finally {
            this.client.run("rmi", this.remote());
        }
    }

    private void buildImage() throws Exception {
        this.client.run("build", this.dockerFile().getParent().toString(), "-t", this.remote());
    }

    private Path dockerFile() {
        return new TestResource("large-image/Dockerfile").asPath();
    }

    private String remote() {
        return String.format("%s/%s", this.repository.url(), LargeImageITCase.IMAGE);
    }
}
