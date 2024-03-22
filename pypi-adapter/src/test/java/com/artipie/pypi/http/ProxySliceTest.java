/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.cache.FromStorageCache;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.BaseResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.SliceSimple;
import com.artipie.scheduling.ProxyArtifactEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

/**
 * Test for {@link ProxySlice}.
 */
class ProxySliceTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test events queue.
     */
    private Queue<ProxyArtifactEvent> events;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.events = new LinkedList<>();
    }

    @Test
    void getsContentFromRemoteAndAdsItToCache() {
        final byte[] body = "some html".getBytes();
        final String key = "index";
        MatcherAssert.assertThat(
            "Returns body from remote",
            new ProxySlice(
                new SliceSimple(
                    BaseResponse.ok().header(ContentType.mime("smth")).body(body)
                ),
                new FromStorageCache(this.storage), Optional.of(this.events), "my-pypi-proxy"
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(body),
                    new RsHasHeaders(
                        ContentType.mime("smth"),
                        new Header("Content-Length", "9")
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/%s", key))
            )
        );
        MatcherAssert.assertThat(
            "Stores index in cache",
            new BlockingStorage(this.storage).value(new Key.From(key)),
            new IsEqual<>(body)
        );
        MatcherAssert.assertThat("Queue has one event", this.events.size() == 1);
    }

    @ParameterizedTest
    @CsvSource({
        "my project versions list in html,text/html,my-project",
        "my project wheel,*,my-project.whl",
        "my project zip,application/zip,my-project.zip",
        "my project tar,application/gzip,my-project.tar.gz"
    })
    void getsFromCacheOnError(final String data, final String header, final String key) {
        final byte[] body = data.getBytes();
        this.storage.save(new Key.From(key), new Content.From(body)).join();
        MatcherAssert.assertThat(
            "Returns body from cache",
            new ProxySlice(
                new SliceSimple(BaseResponse.internalError()),
                new FromStorageCache(this.storage), Optional.of(this.events), "my-pypi-proxy"
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK), new RsHasBody(body),
                    new RsHasHeaders(
                        ContentType.mime(header),
                        new Header("Content-Length", String.valueOf(body.length))
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/%s", key))
            )
        );
        MatcherAssert.assertThat(
            "Data stays intact in cache",
            new BlockingStorage(this.storage).value(new Key.From(key)),
            new IsEqual<>(body)
        );
        MatcherAssert.assertThat("Queue is empty", this.events.isEmpty());
    }

    @Test
    void returnsNotFoundWhenRemoteReturnedBadRequest() {
        MatcherAssert.assertThat(
            "Status 400 returned",
            new ProxySlice(
                new SliceSimple(BaseResponse.badRequest()),
                new FromStorageCache(this.storage), Optional.of(this.events), "my-pypi-proxy"
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/any")
            )
        );
        MatcherAssert.assertThat(
            "Cache storage is empty",
            this.storage.list(Key.ROOT).join().isEmpty(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat("Queue is empty", this.events.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "My_Project,my-project",
        "My.Project.whl,My.Project.whl",
        "Johns.Project.tar.gz,Johns.Project.tar.gz",
        "AnotherIndex,anotherindex"
    })
    void normalisesNamesWhenNecessary(final String line, final String key) {
        final byte[] body = "python artifact".getBytes();
        MatcherAssert.assertThat(
            "Returns body from remote",
            new ProxySlice(
                new SliceSimple(
                    BaseResponse.ok().header(ContentType.mime("smth")).body(body)
                ),
                new FromStorageCache(this.storage), Optional.empty(), "my-pypi-proxy"
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(body),
                    new RsHasHeaders(
                        ContentType.mime("smth"),
                        new Header("Content-Length", String.valueOf(body.length))
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/%s", line))
            )
        );
        MatcherAssert.assertThat(
            "Stores content in cache",
            new BlockingStorage(this.storage).value(new Key.From(key)),
            new IsEqual<>(body)
        );
    }

    @Test
    void returnsNotFoundOnRemoteAndCacheError() {
        MatcherAssert.assertThat(
            "Status 400 returned",
            new ProxySlice(
                new SliceSimple(BaseResponse.badRequest()),
                (key, remote, cache) ->
                    new FailedCompletionStage<>(
                        new IllegalStateException("Failed to obtain item from cache")
                    ), Optional.empty(), "my-pypi-proxy"
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/anything")
            )
        );
        MatcherAssert.assertThat(
            "Cache storage is empty",
            this.storage.list(Key.ROOT).join().isEmpty(),
            new IsEqual<>(true)
        );
    }

}
