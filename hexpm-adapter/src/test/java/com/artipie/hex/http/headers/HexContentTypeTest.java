/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.http.headers;

import com.artipie.http.Headers;
import com.artipie.http.headers.ContentType;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link HexContentType}.
 *
 * @since 0.2
 */
class HexContentTypeTest {

    @Test
    void shouldFillDefaultValue() {
        final String accept = HexContentType.DEFAULT_TYPE;
        final Headers headers = new HexContentType(new Headers.From()).fill();
        String result = "";
        for (final Map.Entry<String, String> header : headers) {
            if (ContentType.NAME.equals(header.getKey())) {
                result = header.getValue();
            }
        }
        MatcherAssert.assertThat(
            result,
            new IsEqual<>(accept)
        );
    }

    @Test
    void shouldFillFromAcceptHeaderWhenNameInLowerCase() {
        final String accept = "application/vnd.hex+json";
        final Headers rqheader = new Headers.From("accept", accept);
        final Headers headers = new HexContentType(rqheader).fill();
        String result = "";
        for (final Map.Entry<String, String> header : headers) {
            if (ContentType.NAME.equals(header.getKey())) {
                result = header.getValue();
            }
        }
        MatcherAssert.assertThat(
            result,
            new IsEqual<>(accept)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "application/vnd.hex+erlang",
        "application/vnd.hex+json",
        "application/json"
    })
    void shouldFillFromAcceptHeader(final String accept) {
        final Headers rqheader = new Headers.From("Accept", accept);
        final Headers headers = new HexContentType(rqheader).fill();
        String result = "";
        for (final Map.Entry<String, String> header : headers) {
            if (ContentType.NAME.equals(header.getKey())) {
                result = header.getValue();
            }
        }
        MatcherAssert.assertThat(
            result,
            new IsEqual<>(accept)
        );
    }
}
