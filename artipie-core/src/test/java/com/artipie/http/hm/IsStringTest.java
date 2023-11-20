/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IsString}.
 *
 * @since 0.7.2
 */
class IsStringTest {

    @Test
    void shouldMatchEqualString() {
        final Charset charset = StandardCharsets.UTF_8;
        final String string = "\u00F6";
        final IsString matcher = new IsString(
            charset,
            new StringContains(false, string)
        );
        MatcherAssert.assertThat(
            matcher.matches(string.getBytes(charset)),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldNotMatchNotEqualString() {
        MatcherAssert.assertThat(
            new IsString("1").matches("2".getBytes()),
            new IsEqual<>(false)
        );
    }
}
