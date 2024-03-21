/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.Matches;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Test for {@link ResponseMatcher}.
 */
class ResponseMatcherTest {

    @Test
    void matchesStatusAndHeaders() {
        final Header header = new Header("Mood", "sunny");
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.CREATED, header)
                .matches(BaseResponse.created().header(header))
        );
    }

    @Test
    void matchesStatusAndHeadersIterable() {
        Headers headers = Headers.from("X-Name", "value");
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.OK, headers)
                .matches(BaseResponse.ok().headers(headers))
        );
    }

    @Test
    void matchesHeaders() {
        final Header header = new Header("Type", "string");
        Assertions.assertTrue(
            new ResponseMatcher(header)
                .matches(BaseResponse.ok().header(header))
        );
    }

    @Test
    void matchesHeadersIterable() {
        Headers headers = Headers.from("aaa", "bbb");
        Assertions.assertTrue(
            new ResponseMatcher(headers)
                .matches(BaseResponse.ok().headers(headers))
        );
    }

    @Test
    void matchesByteBody() {
        final String body = "111";
        Assertions.assertTrue(
            new ResponseMatcher(body.getBytes())
                .matches(BaseResponse.ok().textBody(body))
        );
    }

    @Test
    void matchesStringBody() {
        final String body = "000";
        Assertions.assertTrue(
            new ResponseMatcher(body, StandardCharsets.UTF_8)
                .matches(BaseResponse.ok().textBody(body))
        );
    }

    @Test
    void matchesStatusAndStringBody() {
        final String body = "def";
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.NOT_FOUND, body, StandardCharsets.UTF_8)
                .matches(BaseResponse.notFound().textBody(body))
        );
    }

    @Test
    void matchesStatusAndByteBody() {
        final String body = "abc";
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.OK, body.getBytes())
                .matches(BaseResponse.ok().textBody(body))
        );
    }

    @Test
    void matchesStatusBodyAndHeaders() {
        final String body = "123";
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.OK, body.getBytes())
                .matches(BaseResponse.ok()
                    .header(new Header("Content-Length", "3"))
                    .textBody(body))
        );
    }

    @Test
    void matchesStatusBodyAndHeadersIterable() {
        Headers headers = Headers.from(new ContentLength("4"));
        final byte[] body = "1234".getBytes();
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.FORBIDDEN, headers, body).matches(
                BaseResponse.forbidden().headers(headers).body(body)
            )
        );
    }

    @Test
    void matchesStatusAndHeaderMatcher() {
        final String header = "Some-header";
        final String value = "Some value";
        final Matcher<? super Map.Entry<String, String>> matcher = new IsHeader(header, value);
        Assertions.assertTrue(
            new ResponseMatcher(RsStatus.ACCEPTED, matcher)
                .matches(BaseResponse.accepted().header(header, value))
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
            new IsNot<>(new Matches<>(BaseResponse.notFound().textBody("hello")))
        );
    }

    @Test
    void matchersBodyMismatches() {
        MatcherAssert.assertThat(
            new ResponseMatcher("yyy"),
            new IsNot<>(new Matches<>(BaseResponse.ok().textBody("YYY")))
        );
    }

    @Test
    void matchersBodyIgnoringCase() {
        MatcherAssert.assertThat(
            new ResponseMatcher(Matchers.equalToIgnoringCase("xxx")),
            new Matches<>(BaseResponse.ok().textBody("XXX"))
        );
    }
}
