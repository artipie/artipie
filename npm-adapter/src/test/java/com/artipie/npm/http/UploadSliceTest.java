/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.RsStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.http.slice.TrimPathSlice;
import com.artipie.npm.Publish;
import com.artipie.scheduling.ArtifactEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

/**
 * UploadSliceTest.
 */
public final class UploadSliceTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test artifact events.
     */
    private Queue<ArtifactEvent> events;

    /**
     * Npm publish implementation.
     */
    private Publish publish;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.events = new LinkedList<>();
        this.publish = new CliPublish(this.storage);
    }

    @Test
    void uploadsFileToRemote() throws Exception {
        final Slice slice = new TrimPathSlice(
            new UploadSlice(
                this.publish, this.storage, Optional.of(this.events), UnpublishPutSliceTest.REPO
            ), "ctx"
        );
        final String json = Json.createObjectBuilder()
            .add("name", "@hello/simple-npm-project")
            .add("_id", "1.0.1")
            .add("readme", "Some text")
            .add("versions", Json.createObjectBuilder())
            .add("dist-tags", Json.createObjectBuilder())
            .add("_attachments", Json.createObjectBuilder())
            .build().toString();
        Assertions.assertEquals(
            RsStatus.OK,
            slice.response(
                RequestLine.from("PUT /ctx/package HTTP/1.1"),
                Headers.EMPTY,
                new Content.From(json.getBytes())
            ).join().status()
        );
        Assertions.assertTrue(
            this.storage.exists(new KeyFromPath("package/meta.json")).get()
        );
        Assertions.assertEquals(1, this.events.size());
    }

    @Test
    void shouldFailForBadRequest() {
        final Slice slice = new TrimPathSlice(
            new UploadSlice(
                this.publish, this.storage, Optional.of(this.events), UnpublishPutSliceTest.REPO
            ),
            "my-repo"
        );
        Assertions.assertThrows(
            Exception.class,
            () -> slice.response(
                RequestLine.from("PUT /my-repo/my-package HTTP/1.1"),
                Headers.EMPTY,
                new Content.From("{}".getBytes())
            ).join()
        );
        Assertions.assertTrue(this.events.isEmpty());
    }
}
