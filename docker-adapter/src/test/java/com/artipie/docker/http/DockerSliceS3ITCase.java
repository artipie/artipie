/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.StoragesLoader;
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
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Integration test for {@link DockerSlice}.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DockerClientSupport
final class DockerSliceS3ITCase {

    @RegisterExtension
    static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * Bucket to use in tests.
     */
    private String bucket;

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
    private Storage storage;

    @BeforeEach
    void setUp(final AmazonS3 client) throws Exception {
        this.bucket = UUID.randomUUID().toString();
        client.createBucket(this.bucket);
        this.storage = StoragesLoader.STORAGES
            .newObject(
                "s3",
                new com.artipie.asto.factory.Config.YamlStorageConfig(
                    Yaml.createYamlMappingBuilder()
                        .add("region", "buck1")
                        .add("bucket", this.bucket)
                        .add("endpoint", String.format("http://localhost:%d", MOCK.getHttpPort()))
                        .add(
                            "credentials",
                            Yaml.createYamlMappingBuilder()
                                .add("type", "basic")
                                .add("accessKeyId", "foo")
                                .add("secretAccessKey", "bar")
                                .build()
                        )
                        .build()
                )
            );
        this.events = new LinkedList<>();
        this.repository = new DockerRepository(
            new DockerSlice(new AstoDocker("test_registry", storage), this.events)
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
        MatcherAssert.assertThat(
            "Repository storage must be empty before test",
            storage.list(Key.ROOT).join().isEmpty()
        );
        final String output = this.client.run("push", this.image.remote());
        MatcherAssert.assertThat(
            output,
            Matchers.allOf(this.layersPushed(), this.manifestPushed())
        );
        MatcherAssert.assertThat(
            "Repository storage must not be empty after test",
            !storage.list(Key.ROOT).join().isEmpty()
        );
        MatcherAssert.assertThat("Events queue has one event", this.events.size() == 1);
    }

    @Test
    void shouldPushExisting() throws Exception {
        MatcherAssert.assertThat(
            "Repository storage must be empty before test",
            storage.list(Key.ROOT).join().isEmpty()
        );
        this.client.run("push", this.image.remote());
        final String output = this.client.run("push", this.image.remote());
        MatcherAssert.assertThat(
            output,
            Matchers.allOf(this.layersAlreadyExist(), this.manifestPushed())
        );
        MatcherAssert.assertThat(
            "Repository storage must not be empty after test",
            !storage.list(Key.ROOT).join().isEmpty()
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
