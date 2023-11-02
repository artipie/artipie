/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.helm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.helm.test.ContentOfIndex;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.cactoos.list.ListOf;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link PushChartSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class PushChartSliceTest {

    /**
     * Storage for tests.
     */
    private Storage storage;

    /**
     * Artifact events.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.events = new ConcurrentLinkedQueue<>();
    }

    @Test
    void shouldNotUpdateAfterUpload() {
        final String tgz = "ark-1.0.1.tgz";
        MatcherAssert.assertThat(
            "Wrong status, expected OK",
            new PushChartSlice(this.storage, Optional.of(this.events), "my-helm"),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, "/?updateIndex=false"),
                Headers.EMPTY,
                new Content.From(new TestResource(tgz).asBytes())
            )
        );
        MatcherAssert.assertThat(
            "Index was generated",
            this.storage.list(Key.ROOT).join(),
            new IsEqual<>(new ListOf<Key>(new Key.From(tgz)))
        );
        MatcherAssert.assertThat("No events were added to queue", this.events.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/?updateIndex=true", "/"})
    void shouldUpdateIndexAfterUpload(final String uri) {
        final String tgz = "ark-1.0.1.tgz";
        MatcherAssert.assertThat(
            "Wrong status, expected OK",
            new PushChartSlice(this.storage, Optional.of(this.events), "test-helm"),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, uri),
                Headers.EMPTY,
                new Content.From(new TestResource(tgz).asBytes())
            )
        );
        MatcherAssert.assertThat(
            "Index was not updated",
            new ContentOfIndex(this.storage).index()
                .entries().keySet(),
            new IsEqual<>(new SetOf<>("ark"))
        );
        MatcherAssert.assertThat("One event was added to queue", this.events.size() == 1);
    }
}
