/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.cactoos.iterable.IterableOf;
import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link RqHeaders}.
 *
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class RqHeadersTest {

    @Test
    void findsAllHeaderValues() {
        final String first = "1";
        final String second = "2";
        MatcherAssert.assertThat(
            "RqHeaders didn't find headers by name",
            new RqHeaders(
                new IterableOf<Map.Entry<String, String>>(
                    new MapEntry<>("x-header", first),
                    new MapEntry<>("Accept", "application/json"),
                    new MapEntry<>("X-Header", second)
                ),
                "X-header"
            ),
            Matchers.contains(first, second)
        );
    }

    @Test
    void findSingleValue() {
        final String value = "text/plain";
        MatcherAssert.assertThat(
            "RqHeaders.Single didn't find expected header",
            new RqHeaders.Single(
                Arrays.asList(
                    new MapEntry<>("Content-type", value),
                    new MapEntry<>("Range", "100")
                ),
                "content-type"
            ).asString(),
            new IsEqual<>(value)
        );
    }

    @Test
    void singleFailsIfNoHeadersFound() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new RqHeaders.Single(Collections.emptyList(), "Empty").asString()
        );
    }

    @Test
    void singleFailsIfMoreThanOneHeader() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new RqHeaders.Single(
                Arrays.asList(
                    new MapEntry<>("Content-length", "1024"),
                    new MapEntry<>("Content-Length", "1025")
                ),
                "content-length"
            ).asString()
        );
    }
}
