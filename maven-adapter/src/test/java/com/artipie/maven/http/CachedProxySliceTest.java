/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.RsStatus;
import com.artipie.http.slice.SliceSimple;
import com.artipie.scheduling.ProxyArtifactEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * Test case for {@link CachedProxySlice}.
 */
final class CachedProxySliceTest {

    /**
     * Artifact events queue.
     */
    private Queue<ProxyArtifactEvent> events;

    @BeforeEach
    void init() {
        this.events = new LinkedList<>();
    }

    @Test
    void loadsCachedContent() {
        final byte[] data = "cache".getBytes();
        MatcherAssert.assertThat(
            new CachedProxySlice(
                (line, headers, body) -> ResponseBuilder.ok().textBody("123").completedFuture(),
                (key, supplier, control) -> CompletableFuture.supplyAsync(
                    () -> Optional.of(new Content.From(data))
                ),
                Optional.of(this.events), "*"
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(data)
                ),
                new RequestLine(RqMethod.GET, "/foo")
            )
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    @Test
    void returnsNotFoundOnRemoteError() {
        MatcherAssert.assertThat(
            new CachedProxySlice(
                new SliceSimple(ResponseBuilder.internalError().build()),
                (key, supplier, control) -> supplier.get(), Optional.of(this.events), "*"
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/any")
            )
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    @Test
    void returnsNotFoundOnRemoteAndCacheError() {
        MatcherAssert.assertThat(
            new CachedProxySlice(
                new SliceSimple(ResponseBuilder.internalError().build()),
                (key, supplier, control)
                    -> new FailedCompletionStage<>(new RuntimeException("Any error")),
                Optional.of(this.events), "*"
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/abc")
            )
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/com/artipie/asto/1.5/asto-1.5.jar",
        "/com/artipie/asto/1.0-SNAPSHOT/asto-1.0-20200520.121003-4.jar",
        "/org/apache/commons/3.6/commons-3.6.pom",
        "/org/test/test-app/0.95/test-app-3.6.war"
    })
    void loadsOriginAndAdds(final String path) {
        final byte[] data = "remote".getBytes();
        MatcherAssert.assertThat(
            new CachedProxySlice(
                (line, headers, body) -> ResponseBuilder.ok().body(data).completedFuture(),
                (key, supplier, control) -> supplier.get(), Optional.of(this.events), "*"
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(data)
                ),
                new RequestLine(RqMethod.GET, path)
            )
        );
        MatcherAssert.assertThat("Events queue has one item", this.events.size() == 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/com/artipie/asto/1.5/asto-1.5-sources.jar",
        "/com/artipie/asto/1.0-SNAPSHOT/asto-1.0-20200520.121003-4.jar.sha1",
        "/org/apache/commons/3.6/commons-3.6-javadoc.pom",
        "/org/test/test-app/maven-metadata.xml"
    })
    void loadsOriginAndDoesNotAddToEvents(final String path) {
        final byte[] data = "remote".getBytes();
        MatcherAssert.assertThat(
            new CachedProxySlice(
                (line, headers, body) -> ResponseBuilder.ok().body(data).completedFuture(),
                (key, supplier, control) -> supplier.get(), Optional.of(this.events), "*"
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(data)
                ),
                new RequestLine(RqMethod.GET, path)
            )
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }
}
