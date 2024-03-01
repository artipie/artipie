/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.files;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.cache.FromRemoteCache;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link FileProxySlice}.
 */
final class FileProxySliceTest {

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void sendEmptyHeadersAndContent() throws Exception {
        final AtomicReference<Iterable<Map.Entry<String, String>>> headers;
        headers = new AtomicReference<>();
        final AtomicReference<byte[]> body = new AtomicReference<>();
        new FileProxySlice(
            new FakeClientSlices(
                (rqline, rqheaders, rqbody) -> {
                    headers.set(rqheaders);
                    return new AsyncResponse(
                        new Content.From(rqbody).asBytesFuture().thenApply(
                            bytes -> {
                                body.set(bytes);
                                return StandardRs.OK;
                            }
                        )
                    );
                }
            ),
            new URI("http://host/path")
        ).response(
            new RequestLine(RqMethod.GET, "/").toString(),
            new Headers.From("X-Name", "Value"),
            new Content.From("data".getBytes())
        ).send(
            (status, rsheaders, rsbody) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Headers are empty",
            headers.get(),
            new IsEmptyIterable<>()
        );
        MatcherAssert.assertThat(
            "Body is empty",
            body.get(),
            new IsEqual<>(new byte[0])
        );
    }

    @Test
    void getsContentFromRemoteAndAdsItToCache() {
        final byte[] body = "some".getBytes();
        final String key = "any";
        MatcherAssert.assertThat(
            "Should returns body from remote",
            new FileProxySlice(
                new SliceSimple(
                    new RsFull(
                        RsStatus.OK, new Headers.From("header", "value"), new Content.From(body)
                    )
                ),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(body),
                    new RsHasHeaders(
                        new Header("header", "value"),
                        new Header("Content-Length", "4"),
                        new Header("Content-Length", "4")
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/%s", key))
            )
        );
        MatcherAssert.assertThat(
            "Does not store data in cache",
            new BlockingStorage(this.storage).value(new Key.From(key)),
            new IsEqual<>(body)
        );
    }

    @Test
    void getsFromCacheOnError() {
        final byte[] body = "abc123".getBytes();
        final String key = "any";
        this.storage.save(new Key.From(key), new Content.From(body)).join();
        MatcherAssert.assertThat(
            "Does not return body from cache",
            new FileProxySlice(
                new SliceSimple(new RsWithStatus(RsStatus.INTERNAL_ERROR)),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK), new RsHasBody(body),
                    new RsHasHeaders(
                        new Header("Content-Length", String.valueOf(body.length))
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/%s", key))
            )
        );
        MatcherAssert.assertThat(
            "Data should stays intact in cache",
            new BlockingStorage(this.storage).value(new Key.From(key)),
            new IsEqual<>(body)
        );
    }

    @Test
    void returnsNotFoundWhenRemoteReturnedBadRequest() {
        MatcherAssert.assertThat(
            "Incorrect status, 404 is expected",
            new FileProxySlice(
                new SliceSimple(new RsWithStatus(RsStatus.BAD_REQUEST)),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/any")
            )
        );
        MatcherAssert.assertThat(
            "Cache storage is not empty",
            this.storage.list(Key.ROOT).join().isEmpty(),
            new IsEqual<>(true)
        );
    }

    /**
     * Fake {@link ClientSlices} implementation that returns specified result.
     *
     * @since 0.7
     */
    private static final class FakeClientSlices implements ClientSlices {

        /**
         * Slice returned by requests.
         */
        private final Slice result;

        /**
         * Ctor.
         *
         * @param result Slice returned by requests.
         */
        FakeClientSlices(final Slice result) {
            this.result = result;
        }

        @Override
        public Slice http(final String host) {
            return this.result;
        }

        @Override
        public Slice http(final String host, final int port) {
            return this.result;
        }

        @Override
        public Slice https(final String host) {
            return this.result;
        }

        @Override
        public Slice https(final String host, final int port) {
            return this.result;
        }
    }
}
