/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import io.reactivex.Flowable;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link ContentLengthRestriction}.
 *
 * @since 0.2
 */
class ContentLengthRestrictionTest {

    @Test
    public void shouldNotPassRequestsAboveLimit() {
        final int limit = 10;
        final Slice slice = new ContentLengthRestriction(
            (line, headers, body) -> new RsWithStatus(RsStatus.OK),
            limit
        );
        final Response response = slice.response("", this.headers("11"), Flowable.empty());
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.PAYLOAD_TOO_LARGE));
    }

    @ParameterizedTest
    @CsvSource({"10,0", "10,not number", "10,1", "10,10"})
    public void shouldPassRequestsWithinLimit(final int limit, final String value) {
        final Slice slice = new ContentLengthRestriction(
            (line, headers, body) -> new RsWithStatus(RsStatus.OK),
            limit
        );
        final Response response = slice.response("", this.headers(value), Flowable.empty());
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.OK));
    }

    @Test
    public void shouldPassRequestsWithoutContentLength() {
        final int limit = 10;
        final Slice slice = new ContentLengthRestriction(
            (line, headers, body) -> new RsWithStatus(RsStatus.OK),
            limit
        );
        final Response response = slice.response("", Collections.emptySet(), Flowable.empty());
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.OK));
    }

    private Headers.From headers(final String value) {
        return new Headers.From("Content-Length", value);
    }
}
