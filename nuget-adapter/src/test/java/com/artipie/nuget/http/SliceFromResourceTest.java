/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Tests for {@link SliceFromResource}.
 */
public class SliceFromResourceTest {

    @Test
    void shouldDelegateGetResponse() {
        final Header header = new Header("Name", "Value");
        final byte[] body = "body".getBytes();
        final ResponseImpl response = new SliceFromResource(
            new Resource() {
                @Override
                public CompletableFuture<ResponseImpl> get(final Headers headers) {
                    return ResponseBuilder.ok().headers(headers)
                        .body(body).completedFuture();
                }

                @Override
                public CompletableFuture<ResponseImpl> put(Headers headers, Content body) {
                    throw new UnsupportedOperationException();
                }
            }
        ).response(
            new RequestLine(RqMethod.GET, "/some/path"),
            Headers.from(Collections.singleton(header)),
            Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.OK, response.status());
        Assertions.assertArrayEquals(body, response.body().asBytes());
        MatcherAssert.assertThat(
            response.headers(),
            Matchers.containsInRelativeOrder(header)
        );
    }

    @Test
    void shouldDelegatePutResponse() {
        final Header header = new Header("X-Name", "Something");
        final byte[] content = "content".getBytes();
        final ResponseImpl response = new SliceFromResource(
            new Resource() {
                @Override
                public CompletableFuture<ResponseImpl> get(Headers headers) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public CompletableFuture<ResponseImpl> put(Headers headers, Content body) {
                    return ResponseBuilder.ok().headers(headers)
                        .body(body).completedFuture();
                }
            }
        ).response(
            new RequestLine(RqMethod.PUT, "/some/other/path"),
            Headers.from(Collections.singleton(header)),
            new Content.From(content)
        ).join();
        Assertions.assertEquals(RsStatus.OK, response.status());
        Assertions.assertArrayEquals(content, response.body().asBytes());
        MatcherAssert.assertThat(
            response.headers(),
            Matchers.containsInRelativeOrder(header)
        );
    }
}
