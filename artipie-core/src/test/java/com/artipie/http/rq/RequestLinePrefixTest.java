/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq;

import com.artipie.http.Headers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link RequestLinePrefix}.
 * @since 0.16
 */
class RequestLinePrefixTest {

    @ParameterizedTest
    @CsvSource({
        "/one/two/three,/three,/one/two",
        "/one/two/three,/two/three,/one",
        "/one/two/three,'',/one/two/three",
        "/one/two/three,/,/one/two/three",
        "/one/two,/two/,/one",
        "'',/test,''",
        "'','',''"
    })
    void returnsPrefix(final String full, final String line, final String res) {
        MatcherAssert.assertThat(
            new RequestLinePrefix(line, new Headers.From("X-FullPath", full)).get(),
            new IsEqual<>(res)
        );
    }
}
