/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.http.Slice;
import com.artipie.http.hm.AssertSlice;
import com.artipie.http.hm.RqHasHeader;
import com.artipie.http.hm.RqLineHasUri;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.StandardRs;
import io.reactivex.Flowable;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

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
        new TrimPathSlice((line, headers, body) -> StandardRs.EMPTY, "none").response(
            requestLine("http://www.w3.org").toString(),
            Collections.emptyList(),
            Flowable.empty()
        ).send(
            (status, headers, body) -> {
                MatcherAssert.assertThat(
                    "Not failed",
                    status,
                    IsEqual.equalTo(RsStatus.INTERNAL_ERROR)
                );
                return CompletableFuture.allOf();
            }
        ).toCompletableFuture().get();
    }

    @Test
    void replacesFirstPartOfAbsoluteUriPath() throws Exception {
        verify(
            new TrimPathSlice(
                new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath("/three"))),
                "/one/two/"
            ),
            requestLine("/one/two/three")
        );
    }

    @Test
    void replaceFullUriPath() throws Exception {
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
    void appendsFullPathHeaderToRequest() throws Exception {
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
    void trimPathByPattern() throws Exception {
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
    void dontTrimTwice() throws Exception {
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

    private static void verify(final Slice slice, final RequestLine line) throws Exception {
        slice.response(line.toString(), Collections.emptyList(), Flowable.empty())
            .send((status, headers, body) -> CompletableFuture.completedFuture(null))
            .toCompletableFuture()
            .get();
    }
}
