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

import com.artipie.http.hm.AssertSlice;
import com.artipie.http.hm.RqLineHasUri;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import io.reactivex.Flowable;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link DockerRoutingSlice}.
 *
 * @since 0.9
 */
final class DockerRoutingSliceTest {

    @Test
    void removesDockerPrefix() throws Exception {
        verify(
            new DockerRoutingSlice(
                new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath("/foo/bar")))
            ),
            "/v2/foo/bar"
        );
    }

    @Test
    void ignoresNonDockerRequests() throws Exception {
        final String path = "/repo/name";
        verify(
            new DockerRoutingSlice(
                new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath(path)))
            ),
            path
        );
    }

    @Test
    void emptyDockerRequest() throws Exception {
        verify(
            new DockerRoutingSlice(
                new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath("")))
            ),
            "/v2"
        );
    }

    @Test
    void revertsDockerRequest() throws Exception {
        final String path = "/v2/one/two";
        verify(
            new DockerRoutingSlice(
                new DockerRoutingSlice.Reverted(
                    new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath(path)))
                )
            ),
            path
        );
    }

    private static void verify(final Slice slice, final String path) throws Exception {
        slice.response(
            new RequestLine(RqMethod.GET, path).toString(),
            Collections.emptyList(), Flowable.empty()
        ).send(
            (status, headers, body) -> CompletableFuture.completedFuture(null)
        ).toCompletableFuture().get();
    }
}
