/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.http.Headers;
import com.artipie.http.rq.RequestLine;
import org.cactoos.map.MapEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

/**
 * Test for {@link RtRule.ByHeader}.
 */
class RtRuleByHeaderTest {

    @Test
    void trueIfHeaderIsPresent() {
        final String name = "some header";
        Assertions.assertTrue(
            new RtRule.ByHeader(name).apply(
                new RequestLine("GET", "/"), new Headers.From(new MapEntry<>(name, "any value"))
            )
        );
    }

    @Test
    void falseIfHeaderIsNotPresent() {
        Assertions.assertFalse(
            new RtRule.ByHeader("my header").apply(null, Headers.EMPTY)
        );
    }

    @Test
    void trueIfHeaderIsPresentAndValueMatchesRegex() {
        final String name = "content-type";
        Assertions.assertTrue(
            new RtRule.ByHeader(name, Pattern.compile("text/html.*")).apply(
                new RequestLine("GET", "/some/path"), new Headers.From(new MapEntry<>(name, "text/html; charset=utf-8"))
            )
        );
    }

    @Test
    void falseIfHeaderIsPresentAndValueDoesNotMatchesRegex() {
        final String name = "Accept-Encoding";
        Assertions.assertFalse(
            new RtRule.ByHeader(name, Pattern.compile("gzip.*")).apply(
                new RequestLine("GET", "/another/path"), new Headers.From(new MapEntry<>(name, "deflate"))
            )
        );
    }

}
