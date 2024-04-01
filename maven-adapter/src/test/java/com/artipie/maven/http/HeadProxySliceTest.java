/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import com.artipie.http.slice.SliceSimple;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link HeadProxySlice}.
 */
class HeadProxySliceTest {

    @Test
    void performsRequestWithEmptyHeaderAndBody() {
        new HeadProxySlice(new SliceSimple(ResponseBuilder.ok().build())).response(
            RequestLine.from("HEAD /some/path HTTP/1.1"),
            Headers.from("some", "value"),
            new Content.From("000".getBytes())
        ).thenAccept(resp -> {
            Assertions.assertTrue(resp.headers().isEmpty());
            Assertions.assertEquals(0, resp.body().asBytes().length);
        });
    }

    @Test
    void passesStatusAndHeadersFromResponse() {
        final Headers headers = Headers.from("abc", "123");
        MatcherAssert.assertThat(
            new HeadProxySlice(
                new SliceSimple(ResponseBuilder.created().header("abc", "123").build())
            ),
            new SliceHasResponse(
                Matchers.allOf(new RsHasStatus(RsStatus.CREATED), new RsHasHeaders(headers)),
                new RequestLine(RqMethod.HEAD, "/")
            )
        );
    }

}
