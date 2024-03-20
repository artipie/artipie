/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RsHasHeaders}.
 *
 * @since 0.8
 */
class RsHasHeadersTest {

    @Test
    void shouldMatchHeaders() {
        final MapEntry<String, String> type = new MapEntry<>(
            "Content-Type", "application/json"
        );
        final MapEntry<String, String> length = new MapEntry<>(
            "Content-Length", "123"
        );
        final Response response = new RsWithHeaders(
            new RsWithStatus(RsStatus.OK),
            Headers.from(type, length)
        );
        final RsHasHeaders matcher = new RsHasHeaders(Headers.from(length, type));
        MatcherAssert.assertThat(
            matcher.matches(response),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldMatchOneHeader() {
        Header header = new Header("header1", "value1");
        final Response response = new RsWithHeaders(
            new RsWithStatus(RsStatus.OK),
            Headers.from(
                header,
                new Header("header2", "value2"),
                new Header("header3", "value3")
            )
        );
        final RsHasHeaders matcher = new RsHasHeaders(header);
        MatcherAssert.assertThat(
            matcher.matches(response),
            new IsEqual<>(true)
        );
    }
}
