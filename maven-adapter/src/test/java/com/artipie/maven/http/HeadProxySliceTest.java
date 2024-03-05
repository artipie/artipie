/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

/**
 * Test for {@link HeadProxySlice}.
 */
class HeadProxySliceTest {

    @Test
    void performsRequestWithEmptyHeaderAndBody() {
        new HeadProxySlice(new SliceSimple(StandardRs.EMPTY)).response(
            "HEAD /some/path HTTP/1.1",
            new Headers.From("some", "value"),
            new Content.From("000".getBytes())
        ).send(
            (status, headers, body) -> {
                MatcherAssert.assertThat(
                    "Headers are empty",
                    headers,
                    new IsEmptyIterable<>()
                );
                MatcherAssert.assertThat(
                    "Body is empty",
                    new Content.From(body).asBytes(),
                    new IsEqual<>(new byte[]{})
                );
                return CompletableFuture.allOf();
            }
        );
    }

    @Test
    void passesStatusAndHeadersFromResponse() {
        final RsStatus status = RsStatus.CREATED;
        final Headers.From headers = new Headers.From("abc", "123");
        MatcherAssert.assertThat(
            new HeadProxySlice(
                new SliceSimple(new RsWithHeaders(new RsWithStatus(status), headers))
            ),
            new SliceHasResponse(
                Matchers.allOf(new RsHasStatus(status), new RsHasHeaders(headers)),
                new RequestLine(RqMethod.HEAD, "/")
            )
        );
    }

}
