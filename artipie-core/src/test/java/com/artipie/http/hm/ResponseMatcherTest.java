/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.Matches;

/**
 * Test for {@link ResponseMatcher}.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
class ResponseMatcherTest {

    @Test
    void matchesStatusAndHeaders() {
        final Header header = new Header("Mood", "sunny");
        final RsStatus status = RsStatus.CREATED;
        MatcherAssert.assertThat(
            new ResponseMatcher(RsStatus.CREATED, header)
                .matches(
                    new RsWithHeaders(new RsWithStatus(status), header)
                ),
            new IsEqual<>(true)
        );
    }

    @Test
    void matchesStatusAndHeadersIterable() {
        final Iterable<Map.Entry<String, String>> headers = new Headers.From("X-Name", "value");
        final RsStatus status = RsStatus.OK;
        MatcherAssert.assertThat(
            new ResponseMatcher(RsStatus.OK, headers).matches(
                new RsWithHeaders(new RsWithStatus(status), headers)
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void matchesHeaders() {
        final Header header = new Header("Type", "string");
        MatcherAssert.assertThat(
            new ResponseMatcher(header)
                .matches(
                    new RsWithHeaders(StandardRs.EMPTY, header)
                ),
            new IsEqual<>(true)
        );
    }

    @Test
    void matchesHeadersIterable() {
        final Iterable<Map.Entry<String, String>> headers = new Headers.From("aaa", "bbb");
        MatcherAssert.assertThat(
            new ResponseMatcher(headers).matches(
                new RsWithHeaders(StandardRs.EMPTY, headers)
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void matchesByteBody() {
        final String body = "111";
        MatcherAssert.assertThat(
            new ResponseMatcher(body.getBytes())
                .matches(
                    new RsWithBody(
                        StandardRs.EMPTY, body, StandardCharsets.UTF_8
                    )
                ),
            new IsEqual<>(true)
        );
    }

    @Test
    void matchesStringBody() {
        final String body = "000";
        MatcherAssert.assertThat(
            new ResponseMatcher(body, StandardCharsets.UTF_8)
                .matches(
                    new RsWithBody(
                        StandardRs.EMPTY, body, StandardCharsets.UTF_8
                    )
                ),
            new IsEqual<>(true)
        );
    }

    @Test
    void matchesStatusAndStringBody() {
        final String body = "def";
        MatcherAssert.assertThat(
            new ResponseMatcher(RsStatus.NOT_FOUND, body, StandardCharsets.UTF_8)
                .matches(
                    new RsWithBody(
                        StandardRs.NOT_FOUND, body, StandardCharsets.UTF_8
                    )
                ),
            new IsEqual<>(true)
        );
    }

    @Test
    void matchesStatusAndByteBody() {
        final String body = "abc";
        MatcherAssert.assertThat(
            new ResponseMatcher(RsStatus.OK, body.getBytes())
                .matches(
                    new RsWithBody(
                        StandardRs.EMPTY, body, StandardCharsets.UTF_8
                    )
                ),
            new IsEqual<>(true)
        );
    }

    @Test
    void matchesStatusBodyAndHeaders() {
        final String body = "123";
        MatcherAssert.assertThat(
            new ResponseMatcher(RsStatus.OK, body.getBytes())
                .matches(
                    new RsWithBody(
                        new RsWithHeaders(
                            StandardRs.EMPTY,
                            new Header("Content-Length", "3")
                        ),
                        body, StandardCharsets.UTF_8
                    )
                ),
            new IsEqual<>(true)
        );
    }

    @Test
    void matchesStatusBodyAndHeadersIterable() {
        final RsStatus status = RsStatus.FORBIDDEN;
        final Iterable<Map.Entry<String, String>> headers = new Headers.From(
            new ContentLength("4")
        );
        final byte[] body = "1234".getBytes();
        MatcherAssert.assertThat(
            new ResponseMatcher(status, headers, body).matches(
                new RsFull(status, headers, Flowable.just(ByteBuffer.wrap(body)))
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void matchesStatusAndHeaderMatcher() {
        final RsStatus status = RsStatus.ACCEPTED;
        final String header = "Some-header";
        final String value = "Some value";
        final Matcher<? super Map.Entry<String, String>> matcher = new IsHeader(header, value);
        MatcherAssert.assertThat(
            new ResponseMatcher(status, matcher)
                .matches(
                    new RsWithHeaders(
                        new RsWithStatus(status),
                        new Headers.From(header, value)
                    )
                ),
            new IsEqual<>(true)
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
            new IsNot<>(
                new Matches<>(
                    new RsFull(
                        RsStatus.NOT_FOUND,
                        Headers.EMPTY,
                        new Content.From(
                            "hello".getBytes(StandardCharsets.UTF_8)
                        )
                    )
                )
            )
        );
    }

    @Test
    void matchersBodyMismatches() {
        MatcherAssert.assertThat(
            new ResponseMatcher("yyy"),
            new IsNot<>(
                new Matches<>(
                    new RsWithBody("YYY", StandardCharsets.UTF_8)
                )
            )
        );
    }

    @Test
    void matchersBodyIgnoringCase() {
        MatcherAssert.assertThat(
            new ResponseMatcher(
                Matchers.equalToIgnoringCase("xxx")
            ),
            new Matches<>(
                new RsWithBody("XXX", StandardCharsets.UTF_8)
            )
        );
    }
}
