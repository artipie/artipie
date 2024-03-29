/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.RsStatus;
import com.artipie.http.Slice;
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
    void changesOnlyUriPath() throws Exception {
        verify(
            new TrimPathSlice(
                new AssertSlice(
                    new RqLineHasUri(
                        new IsEqual<>(URI.create("http://www.w3.org/WWW/TheProject.html"))
                    )
                ),
                "pub/"
            ),
            requestLine("http://www.w3.org/pub/WWW/TheProject.html")
        );
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
        verify(
            new TrimPathSlice(
                new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath("/three"))),
                "/one/two/"
            ),
            requestLine("/one/two/three")
        );
    }

    @Test
    void replaceFullUriPath() {
        final String path = "/foo/bar";
        verify(
            new TrimPathSlice(
                new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath("/"))),
                path
            ),
            requestLine(path)
        );
    }

    @Test
    void appendsFullPathHeaderToRequest() {
        final String path = "/a/b/c";
        verify(
            new TrimPathSlice(
                new AssertSlice(
                    Matchers.anything(),
                    new RqHasHeader.Single("x-fullpath", path),
                    Matchers.anything()
                ),
                "/a/b"
            ),
            requestLine(path)
        );
    }

    @Test
    void trimPathByPattern() {
        final String path = "/repo/version/artifact";
        verify(
            new TrimPathSlice(
                new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath("/version/artifact"))),
                Pattern.compile("/[a-zA-Z0-9]+/")
            ),
            requestLine(path)
        );
    }

    @Test
    void dontTrimTwice() {
        final String prefix = "/one";
        verify(
            new TrimPathSlice(
                new TrimPathSlice(
                    new AssertSlice(
                        new RqLineHasUri(new RqLineHasUri.HasPath("/one/two"))
                    ),
                    prefix
                ),
                prefix
            ),
            requestLine("/one/one/two")
        );
    }

    private static RequestLine requestLine(final String path) {
        return new RequestLine("GET", path, "HTTP/1.1");
    }

    private static void verify(final Slice slice, final RequestLine line) {
        slice.response(line, Headers.EMPTY, Content.EMPTY).join();
    }
}
