/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.RsStatus;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ResponseMatcher}.
 */
class ResponseMatcherTest {

    @Test
    void matchesStatusAndHeaders() {
        final Header header = new Header("Mood", "sunny");
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.CREATED, header)
                .matches(ResponseBuilder.created().header(header).build())
        );
    }

    @Test
    void matchesStatusAndHeadersIterable() {
        Headers headers = Headers.from("X-Name", "value");
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.OK, headers)
                .matches(ResponseBuilder.ok().headers(headers).build())
        );
    }

    @Test
    void matchesHeaders() {
        final Header header = new Header("Type", "string");
        Assertions.assertTrue(
            new ResponseMatcher(header)
                .matches(ResponseBuilder.ok().header(header).build())
        );
    }

    @Test
    void matchesHeadersIterable() {
        Headers headers = Headers.from("aaa", "bbb");
        Assertions.assertTrue(
            new ResponseMatcher(headers)
                .matches(ResponseBuilder.ok().headers(headers).build())
        );
    }

    @Test
    void matchesByteBody() {
        final String body = "111";
        Assertions.assertTrue(
            new ResponseMatcher(body.getBytes())
                .matches(ResponseBuilder.ok().textBody(body).build())
        );
    }

    @Test
    void matchesStatusAndByteBody() {
        final String body = "abc";
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.OK, body.getBytes())
                .matches(ResponseBuilder.ok().textBody(body).build())
        );
    }

    @Test
    void matchesStatusBodyAndHeaders() {
        final String body = "123";
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.OK, body.getBytes())
                .matches(ResponseBuilder.ok()
                    .header(new Header("Content-Length", "3"))
                    .textBody(body)
                    .build())
        );
    }

    @Test
    void matchesStatusBodyAndHeadersIterable() {
        Headers headers = Headers.from(new ContentLength("4"));
        final byte[] body = "1234".getBytes();
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.FORBIDDEN, headers, body).matches(
                ResponseBuilder.forbidden().headers(headers)
                    .body(body).build()
            )
        );
    }
}
