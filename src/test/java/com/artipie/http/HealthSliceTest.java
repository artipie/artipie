/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.test.TestSettings;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link HealthSlice}.
 *
 * @since 0.10
 */
final class HealthSliceTest {
    /**
     * Request line for health endpoint.
     */
    private static final RequestLine REQ_LINE = new RequestLine(RqMethod.GET, "/.health");

    @Test
    void returnsOkForValidStorage() {
        MatcherAssert.assertThat(
            new HealthSlice(new TestSettings()),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody("[{\"storage\":\"ok\"}]", StandardCharsets.UTF_8)
                ),
                HealthSliceTest.REQ_LINE
            )
        );
    }

    @Test
    void returnsBadRequestForBrokenStorage() {
        MatcherAssert.assertThat(
            new HealthSlice(new TestSettings(new FakeStorage())),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.UNAVAILABLE),
                    new RsHasBody("[{\"storage\":\"failure\"}]", StandardCharsets.UTF_8)
                ),
                HealthSliceTest.REQ_LINE
            )
        );
    }

    /**
     * Implementation of broken storage.
     * All methods throw exception.
     *
     * @since 0.10
     */
    private static class FakeStorage implements Storage {
        @Override
        public CompletableFuture<Boolean> exists(final Key key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Collection<Key>> list(final Key prefix) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Void> save(final Key key, final Content content) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Void> move(final Key source, final Key destination) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<? extends Meta> metadata(final Key key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Content> value(final Key key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Void> delete(final Key key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletionStage<T> exclusively(
            final Key key,
            final Function<Storage, CompletionStage<T>> function) {
            throw new UnsupportedOperationException();
        }
    }
}
