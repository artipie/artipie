/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.http.headers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
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
        "abc:xyz,abc:XYZ,true",
        "abc:xyz,abc:xyz ,true"
    })
    void shouldBeEqual(final String one, final String another) {
        Assertions.assertEquals(fromString(one), fromString(another));
        Assertions.assertEquals(fromString(one).hashCode(), fromString(another).hashCode());
    }

    @ParameterizedTest
    @CsvSource({
        "abc:xyz,foo:bar",
        "abc:xyz,abc:bar",
        "abc:xyz,foo:xyz",
    })
    void shouldNotBeEqual(final String one, final String another) {
        Assertions.assertNotEquals(fromString(one), fromString(another));
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

    private static Header fromString(final String raw) {
        final String[] split = raw.split(":");
        return new Header(split[0], split[1]);
    }

}
