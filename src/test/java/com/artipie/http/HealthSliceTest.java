/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.http;

import com.artipie.Settings;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.Transaction;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
            new HealthSlice(new Settings.Fake()),
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
            new HealthSlice(new Settings.Fake(new FakeStorage())),
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
        public CompletableFuture<Long> size(final Key key) {
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
        public CompletableFuture<Transaction> transaction(final List<Key> keys) {
            throw new UnsupportedOperationException();
        }
    }
}
