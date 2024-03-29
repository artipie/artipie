/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AuthClientSlice}.
 */
class AuthClientSliceTest {

    @Test
    void shouldNotModifyRequestAndResponseIfNoAuthRequired() {
        final RequestLine line = new RequestLine(RqMethod.GET, "/file.txt");
        final Header header = new Header("x-name", "some value");
        final byte[] body = "text".getBytes();
        final ResponseImpl response = new AuthClientSlice(
            (rsline, rsheaders, rsbody) -> {
                if (!rsline.equals(line)) {
                    throw new IllegalArgumentException(String.format("Line modified: %s", rsline));
                }
                return ResponseBuilder.ok()
                    .headers(rsheaders)
                    .body(rsbody)
                    .completedFuture();
            },
            Authenticator.ANONYMOUS
        ).response(line, Headers.from(header), new Content.From(body)).join();
        MatcherAssert.assertThat(
            response,
            new ResponseMatcher(RsStatus.OK, body, header)
        );
    }
}
