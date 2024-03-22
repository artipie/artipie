/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link ContentLengthRestriction}.
 */
class ContentLengthRestrictionTest {

    @Test
    public void shouldNotPassRequestsAboveLimit() {
        final Slice slice = new ContentLengthRestriction(
            (line, headers, body) -> BaseResponse.ok(), 10
        );
        final Response response = slice.response(new RequestLine("GET", "/"), this.headers("11"), Content.EMPTY);
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.PAYLOAD_TOO_LARGE));
    }

    @ParameterizedTest
    @CsvSource({"10,0", "10,not number", "10,1", "10,10"})
    public void shouldPassRequestsWithinLimit(int limit, String value) {
        final Slice slice = new ContentLengthRestriction(
            (line, headers, body) -> BaseResponse.ok(), limit
        );
        final Response response = slice.response(new RequestLine("GET", "/"), this.headers(value), Content.EMPTY);
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.OK));
    }

    @Test
    public void shouldPassRequestsWithoutContentLength() {
        final Slice slice = new ContentLengthRestriction(
            (line, headers, body) -> BaseResponse.ok(), 10
        );
        final Response response = slice.response(new RequestLine("GET", "/"), Headers.EMPTY, Content.EMPTY);
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.OK));
    }

    private Headers headers(final String value) {
        return Headers.from("Content-Length", value);
    }
}
