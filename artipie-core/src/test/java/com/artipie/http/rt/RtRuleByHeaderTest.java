/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.http.Headers;
import java.util.regex.Pattern;
import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RtRule.ByHeader}.
 * @since 0.17
 */
class RtRuleByHeaderTest {

    @Test
    void trueIfHeaderIsPresent() {
        final String name = "some header";
        MatcherAssert.assertThat(
            new RtRule.ByHeader(name).apply(
                "what ever", new Headers.From(new MapEntry<>(name, "any value"))
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void falseIfHeaderIsNotPresent() {
        MatcherAssert.assertThat(
            new RtRule.ByHeader("my header").apply("rq line", Headers.EMPTY),
            new IsEqual<>(false)
        );
    }

    @Test
    void trueIfHeaderIsPresentAndValueMatchesRegex() {
        final String name = "content-type";
        MatcherAssert.assertThat(
            new RtRule.ByHeader(name, Pattern.compile("text/html.*")).apply(
                "/some/path", new Headers.From(new MapEntry<>(name, "text/html; charset=utf-8"))
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void falseIfHeaderIsPresentAndValueDoesNotMatchesRegex() {
        final String name = "Accept-Encoding";
        MatcherAssert.assertThat(
            new RtRule.ByHeader(name, Pattern.compile("gzip.*")).apply(
                "/another/path", new Headers.From(new MapEntry<>(name, "deflate"))
            ),
            new IsEqual<>(false)
        );
    }

}
