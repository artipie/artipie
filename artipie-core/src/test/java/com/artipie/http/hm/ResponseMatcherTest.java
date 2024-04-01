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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.Matches;

import java.nio.charset.StandardCharsets;

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
    void matchesStringBody() {
        final String body = "000";
        Assertions.assertTrue(
            new ResponseMatcher(body, StandardCharsets.UTF_8)
                .matches(ResponseBuilder.ok().textBody(body).build())
        );
    }

    @Test
    void matchesStatusAndStringBody() {
        final String body = "def";
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.NOT_FOUND, body, StandardCharsets.UTF_8)
                .matches(ResponseBuilder.notFound().textBody(body).build())
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

    @Test
    void matchersBodyAndStatus() {
        MatcherAssert.assertThat(
            new ResponseMatcher(
                RsStatus.NOT_FOUND,
                Matchers.containsString("404"),
                StandardCharsets.UTF_8
            ),
            new IsNot<>(new Matches<>(ResponseBuilder.notFound().textBody("hello").build()))
        );
    }

    @Test
    void matchersBodyMismatches() {
        MatcherAssert.assertThat(
            new ResponseMatcher("yyy"),
            new IsNot<>(new Matches<>(ResponseBuilder.ok().textBody("YYY").build()))
        );
    }

    @Test
    void matchersBodyIgnoringCase() {
        MatcherAssert.assertThat(
            new ResponseMatcher(Matchers.equalToIgnoringCase("xxx")),
            new Matches<>(ResponseBuilder.ok().textBody("XXX").build())
        );
    }
}
