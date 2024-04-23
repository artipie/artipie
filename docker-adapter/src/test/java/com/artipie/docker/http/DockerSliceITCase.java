/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.junit.DockerClient;
import com.artipie.docker.junit.DockerClientSupport;
import com.artipie.docker.junit.DockerRepository;
import com.artipie.scheduling.ArtifactEvent;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Integration test for {@link DockerSlice}.
 */
@DockerClientSupport
final class DockerSliceITCase {
    /**
     * Example docker image to use in tests.
     */
    private Image image;

    /**
     * Docker client.
     */
    private DockerClient client;

    /**
     * Docker repository.
     */
    private DockerRepository repository;

    /**
     * Artifact event.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void setUp() throws Exception {
        this.events = new LinkedList<>();
        this.repository = new DockerRepository(
            new DockerSlice(new AstoDocker("test_registry", new InMemoryStorage()), this.events)
        );
        this.repository.start();
        this.image = this.prepareImage();
    }

    @AfterEach
    void tearDown() {
        this.repository.stop();
    }

    @Test
    void shouldPush() throws Exception {
        final String output = this.client.run("push", this.image.remote());
        MatcherAssert.assertThat(
            output,
            Matchers.allOf(this.layersPushed(), this.manifestPushed())
        );
        MatcherAssert.assertThat("Events queue has one event", this.events.size() == 1);
    }

    @Test
    void shouldPushExisting() throws Exception {
        this.client.run("push", this.image.remote());
        final String output = this.client.run("push", this.image.remote());
        MatcherAssert.assertThat(
            output,
            Matchers.allOf(this.layersAlreadyExist(), this.manifestPushed())
        );
        MatcherAssert.assertThat("Events queue has one event", this.events.size() == 2);
    }

    @Test
    void shouldPullPushedByTag() throws Exception {
        this.client.run("push", this.image.remote());
        this.client.run("image", "rm", this.image.name());
        this.client.run("image", "rm", this.image.remote());
        final String output = this.client.run("pull", this.image.remote());
        MatcherAssert.assertThat(
            output,
            new StringContains(
                false,
                String.format("Status: Downloaded newer image for %s", this.image.remote())
            )
        );
    }

    @Test
    void shouldPullPushedByDigest() throws Exception {
        this.client.run("push", this.image.remote());
        this.client.run("image", "rm", this.image.name());
        this.client.run("image", "rm", this.image.remote());
        final String output = this.client.run("pull", this.image.remoteByDigest());
        MatcherAssert.assertThat(
            output,
            new StringContains(
                false,
                String.format("Status: Downloaded newer image for %s", this.image.remoteByDigest())
            )
        );
    }

    private Image prepareImage() throws Exception {
        final Image tmpimg = new Image.ForOs();
        final String original = tmpimg.remoteByDigest();
        this.client.run("pull", original);
        final String local = "my-test";
        this.client.run("tag", original, String.format("%s:latest", local));
        final Image img = new Image.From(
            this.repository.url(),
            local,
            tmpimg.digest(),
            tmpimg.layer()
        );
        this.client.run("tag", original, img.remote());
        return img;
    }

    private Matcher<String> manifestPushed() {
        return new StringContains(false, String.format("latest: digest: %s", this.image.digest()));
    }

    private Matcher<String> layersPushed() {
        return new StringContains(false, String.format("%s: Pushed", this.image.layer()));
    }

    private Matcher<String> layersAlreadyExist() {
        return new StringContains(
            false,
            String.format("%s: Layer already exists", this.image.layer())
        );
    }
}
