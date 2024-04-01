/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.RsStatus;
import com.artipie.http.hm.AssertSlice;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.hm.RqHasHeader;
import com.artipie.http.hm.RqLineHasUri;
import com.artipie.http.rq.RequestLine;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Test case for {@link TrimPathSlice}.
 * @since 0.8
 */
final class TrimPathSliceTest {

    @Test
    void changesOnlyUriPath() {
        new TrimPathSlice(
            new AssertSlice(
                new RqLineHasUri(
                    new IsEqual<>(URI.create("http://www.w3.org/WWW/TheProject.html"))
                )
            ),
            "pub/"
        ).response(requestLine("http://www.w3.org/pub/WWW/TheProject.html"),
            Headers.EMPTY, Content.EMPTY).join();
    }

    @Test
    void failIfUriPathDoesntMatch() throws Exception {
        ResponseAssert.check(
            new TrimPathSlice((line, headers, body) ->
                CompletableFuture.completedFuture(ResponseBuilder.ok().build()), "none")
                .response(requestLine("http://www.w3.org"), Headers.EMPTY, Content.EMPTY)
                .join(),
            RsStatus.INTERNAL_ERROR
        );
    }

    @Test
    void replacesFirstPartOfAbsoluteUriPath() {
        new TrimPathSlice(
            new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath("/three"))),
            "/one/two/"
        ).response(requestLine("/one/two/three"), Headers.EMPTY, Content.EMPTY).join();
    }

    @Test
    void replaceFullUriPath() {
        final String path = "/foo/bar";
        new TrimPathSlice(
            new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath("/"))),
            path
        ).response(requestLine(path), Headers.EMPTY, Content.EMPTY).join();
    }

    @Test
    void appendsFullPathHeaderToRequest() {
        final String path = "/a/b/c";
        new TrimPathSlice(
            new AssertSlice(
                Matchers.anything(),
                new RqHasHeader.Single("x-fullpath", path),
                Matchers.anything()
            ),
            "/a/b"
        ).response(requestLine(path), Headers.EMPTY, Content.EMPTY).join();
    }

    @Test
    void trimPathByPattern() {
        final String path = "/repo/version/artifact";
        new TrimPathSlice(
            new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath("/version/artifact"))),
            Pattern.compile("/[a-zA-Z0-9]+/")
        ).response(requestLine(path), Headers.EMPTY, Content.EMPTY).join();
    }

    @Test
    void dontTrimTwice() {
        final String prefix = "/one";
        new TrimPathSlice(
            new TrimPathSlice(
                new AssertSlice(
                    new RqLineHasUri(new RqLineHasUri.HasPath("/one/two"))
                ),
                prefix
            ),
            prefix
        ).response(requestLine("/one/one/two"), Headers.EMPTY, Content.EMPTY).join();
    }

    private static RequestLine requestLine(final String path) {
        return new RequestLine("GET", path, "HTTP/1.1");
    }
}
