/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
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
