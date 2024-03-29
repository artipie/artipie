/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.ArtipieException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.npm.JsonFromMeta;
import com.artipie.scheduling.ArtifactEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletionException;

/**
 * Test cases for {@link UnpublishPutSlice}.
 */
final class UnpublishPutSliceTest {

    /**
     * Test repo name.
     */
    static final String REPO = "test-npm";

    /**
     * Test project name.
     */
    private static final String PROJ = "@hello/simple-npm-project";

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Meta file key.
     */
    private Key meta;

    /**
     * Test artifact events.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.meta = new Key.From(UnpublishPutSliceTest.PROJ, "meta.json");
        this.events = new LinkedList<>();
    }

    @Test
    void returnsNotFoundIfMetaIsNotFound() {
        MatcherAssert.assertThat(
            new UnpublishPutSlice(
                this.storage, Optional.of(this.events), UnpublishPutSliceTest.REPO
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.PUT, "/some/project/-rev/undefined"),
                Headers.from("referer", "unpublish"),
                Content.EMPTY
            )
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"time", "versions", "dist-tags"})
    void removeVersionFromAllEntries(final String entry) {
        this.saveSourceMeta();
        MatcherAssert.assertThat(
            "Response status is OK",
            new UnpublishPutSlice(
                this.storage, Optional.of(this.events), UnpublishPutSliceTest.REPO
            ),
            UnpublishPutSliceTest.responseMatcher()
        );
        MatcherAssert.assertThat(
            "Meta.json is updated",
            new JsonFromMeta(
                this.storage, new Key.From(UnpublishPutSliceTest.PROJ)
            ).json()
                .getJsonObject(entry)
                .keySet(),
            new IsNot<>(Matchers.hasItem("1.0.2"))
        );
        MatcherAssert.assertThat("Events queue has one item", this.events.size() == 1);
    }

    @Test
    void decreaseLatestVersion() {
        this.saveSourceMeta();
        MatcherAssert.assertThat(
            "Response status is OK",
            new UnpublishPutSlice(
                this.storage, Optional.of(this.events), UnpublishPutSliceTest.REPO
            ),
            UnpublishPutSliceTest.responseMatcher()
        );
        MatcherAssert.assertThat(
            "Meta.json `dist-tags` are updated",
            new JsonFromMeta(
                this.storage, new Key.From(UnpublishPutSliceTest.PROJ)
            ).json()
                .getJsonObject("dist-tags")
                .getString("latest"),
            new IsEqual<>("1.0.1")
        );
        MatcherAssert.assertThat("Events queue has one item", this.events.size() == 1);
    }

    @Test
    void failsToDeleteMoreThanOneVersion() {
        this.saveSourceMeta();
        final Throwable thr = Assertions.assertThrows(
            CompletionException.class,
            () -> new UnpublishPutSlice(
                this.storage, Optional.of(this.events), UnpublishPutSliceTest.REPO
            ).response(
                RequestLine.from("PUT /@hello%2fsimple-npm-project/-rev/undefined HTTP/1.1"),
                Headers.from("referer", "unpublish"),
                new Content.From(new TestResource("json/dist-tags.json").asBytes())
            ).join()
        );
        MatcherAssert.assertThat(
            thr.getCause(),
            new IsInstanceOf(ArtipieException.class)
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    private void saveSourceMeta() {
        this.storage.save(
            this.meta,
            new Content.From(
                new TestResource("json/unpublish.json").asBytes()
            )
        ).join();
    }

    private static SliceHasResponse responseMatcher() {
        return new SliceHasResponse(
            new RsHasStatus(RsStatus.OK),
            new RequestLine(
                RqMethod.PUT, "/@hello%2fsimple-npm-project/-rev/undefined"
            ),
            Headers.from("referer", "unpublish"),
            new Content.From(
                new TestResource(
                    String.format("storage/%s/meta.json", UnpublishPutSliceTest.PROJ)
                ).asBytes()
            )
        );
    }
}
