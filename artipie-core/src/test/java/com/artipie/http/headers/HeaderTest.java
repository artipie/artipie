/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.headers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test case for {@link Header}.
 *
 * @since 0.1
 */
final class HeaderTest {

    @ParameterizedTest
    @CsvSource({
        "abc:xyz,abc:xyz,true",
        "abc:xyz,ABC:xyz,true",
        "ABC:xyz,abc:xyz,true",
        "abc:xyz,abc: xyz,true",
        "abc:xyz,foo:bar,false",
        "abc:xyz,abc:bar,false",
        "abc:xyz,abc:XYZ,false",
        "abc:xyz,foo:xyz,false",
        "abc:xyz,abc:xyz ,true"
    })
    void shouldBeEqual(final String one, final String another, final boolean equal) {
        MatcherAssert.assertThat(
            fromString(one).equals(fromString(another)),
            new IsEqual<>(equal)
        );
        MatcherAssert.assertThat(
            fromString(one).hashCode() == fromString(another).hashCode(),
            new IsEqual<>(equal)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "abc,abc",
        " abc,abc",
        "\tabc,abc",
        "abc ,abc "
    })
    void shouldTrimValueLeadingWhitespaces(final String original, final String expected) {
        MatcherAssert.assertThat(
            new Header("whatever", original).getValue(),
            new IsEqual<>(expected)
        );
    }

    @Test
    void toStringHeader() throws Exception {
        MatcherAssert.assertThat(
            new Header("name", "value").toString(),
            new IsEqual<>("name: value")
        );
    }

    private static Header fromString(final String raw) {
        final String[] split = raw.split(":");
        return new Header(split[0], split[1]);
    }

}
