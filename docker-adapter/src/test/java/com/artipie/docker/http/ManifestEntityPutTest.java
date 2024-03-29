/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Blob;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.TrustedBlobSource;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Tests for {@link DockerSlice}.
 * Manifest PUT endpoint.
 */
class ManifestEntityPutTest {

    private DockerSlice slice;

    private Docker docker;

    private Queue<ArtifactEvent> events;

    @BeforeEach
    void setUp() {
        this.docker = new AstoDocker(new InMemoryStorage());
        this.events = new LinkedList<>();
        this.slice = new DockerSlice(this.docker, this.events);
    }

    @Test
    void shouldPushManifestByTag() {
        final String path = "/v2/my-alpine/manifests/1";
        ResponseAssert.check(
            this.slice.response(
                new RequestLine(RqMethod.PUT, path), Headers.EMPTY, this.manifest()
            ).join(),
            RsStatus.CREATED,
            new Header("Location", path),
            new Header("Content-Length", "0"),
            new Header(
                "Docker-Content-Digest",
                "sha256:ef0ff2adcc3c944a63f7cafb386abc9a1d95528966085685ae9fab2a1c0bedbf"
            )
        );
        MatcherAssert.assertThat("One event was added to queue", this.events.size() == 1);
        final ArtifactEvent item = this.events.element();
        MatcherAssert.assertThat(item.artifactName(), new IsEqual<>("my-alpine"));
        MatcherAssert.assertThat(item.artifactVersion(), new IsEqual<>("1"));
    }

    @Test
    void shouldPushManifestByDigest() {
        final String digest = String.format(
            "%s:%s",
            "sha256",
            "ef0ff2adcc3c944a63f7cafb386abc9a1d95528966085685ae9fab2a1c0bedbf"
        );
        final String path = String.format("/v2/my-alpine/manifests/%s", digest);
        ResponseAssert.check(
            this.slice.response(
                new RequestLine(RqMethod.PUT, path), Headers.EMPTY, this.manifest()
            ).join(),
            RsStatus.CREATED,
            new Header("Location", path),
            new Header("Content-Length", "0"),
            new Header("Docker-Content-Digest", digest)
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    /**
     * Create manifest content.
     *
     * @return Manifest content.
     */
    private Content manifest() {
        final byte[] content = "config".getBytes();
        final Blob config = this.docker.repo(new RepoName.Valid("my-alpine")).layers()
            .put(new TrustedBlobSource(content))
            .toCompletableFuture().join();
        final byte[] data = String.format(
            "{\"config\":{\"digest\":\"%s\"},\"layers\":[],\"mediaType\":\"my-type\"}",
            config.digest().string()
        ).getBytes();
        return new Content.From(data);
    }
}
