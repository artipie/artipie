/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.cache.FromRemoteCache;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.composer.AstoRepository;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.RsStatus;
import com.artipie.http.slice.SliceSimple;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CachedProxySlice}.
 * @todo #77:30min Enable tests or remove them.
 *  Now caching functionality is not implemented for class because
 *  the index for a specific package is obtained by combining info
 *  local packages file and the remote one. It is necessary to
 *  investigate issue how to cache this information and does it
 *  require to be cached at all. After that enable tests or remove them.
 */
final class CachedProxySliceTest {

    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Disabled
    @Test
    void loadsFromRemoteAndOverrideCachedContent() {
        final byte[] cached = "cache".getBytes();
        final byte[] remote = "remote content".getBytes();
        final Key key = new Key.From("my_key");
        this.storage.save(key, new Content.From(cached)).join();
        MatcherAssert.assertThat(
            "Returns body from remote",
            new CachedProxySlice(
                new SliceSimple(
                    ResponseBuilder.ok().textBody("remote content").build()
                ),
                new AstoRepository(this.storage),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                new AllOf<>(
                    new ListOf<>(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasHeaders(new ContentLength(remote.length)),
                        new RsHasBody(remote)
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/%s", key.string()))
            )
        );
        MatcherAssert.assertThat(
            "Overrides existed value in cache",
            new BlockingStorage(this.storage).value(key),
            new IsEqual<>(remote)
        );
    }

    @Disabled
    @Test
    void getsContentFromRemoteAndCachesIt() {
        final byte[] body = "some info".getBytes();
        final String key = "key";
        MatcherAssert.assertThat(
            "Returns body from remote",
            new CachedProxySlice(
                new SliceSimple(
                    ResponseBuilder.ok().textBody("some info").build()
                ),
                new AstoRepository(this.storage),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                new RsHasBody(body),
                new RequestLine(RqMethod.GET, String.format("/%s", key))
            )
        );
        MatcherAssert.assertThat(
            "Stores value in cache",
            new BlockingStorage(this.storage).value(new Key.From(key)),
            new IsEqual<>(body)
        );
    }

    @Disabled
    @Test
    void getsFromCacheOnRemoteSliceError() {
        final byte[] body = "some data".getBytes();
        final Key key = new Key.From("key");
        new BlockingStorage(this.storage).save(key, body);
        MatcherAssert.assertThat(
            "Returns body from cache",
            new CachedProxySlice(
                new SliceSimple(ResponseBuilder.internalError().build()),
                new AstoRepository(this.storage),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                new AllOf<>(
                    new ListOf<>(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasHeaders(new ContentLength(body.length)),
                        new RsHasBody(body)
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/%s", key.string()))
            )
        );
        MatcherAssert.assertThat(
            "Data is intact in cache",
            new BlockingStorage(this.storage).value(key),
            new IsEqual<>(body)
        );
    }

    @Test
    void returnsNotFoundWhenRemoteReturnedBadRequest() {
        MatcherAssert.assertThat(
            "Status 400 is returned",
            new CachedProxySlice(
                new SliceSimple(ResponseBuilder.badRequest().build()),
                new AstoRepository(this.storage),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/any/request")
            )
        );
        this.assertEmptyCache();
    }

    @Test
    void returnsNotFoundOnRemoteAndCacheError() {
        MatcherAssert.assertThat(
            "Status is 400 returned",
            new CachedProxySlice(
                new SliceSimple(ResponseBuilder.badRequest().build()),
                new AstoRepository(this.storage),
                (key, remote, cache) ->
                    new FailedCompletionStage<>(
                        new IllegalStateException("Failed to obtain item from cache")
                    )
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/any")
            )
        );
        this.assertEmptyCache();
    }

    private void assertEmptyCache() {
        MatcherAssert.assertThat(
            "Cache storage is empty",
            this.storage.list(Key.ROOT)
                .join().isEmpty(),
            new IsEqual<>(true)
        );
    }
}
