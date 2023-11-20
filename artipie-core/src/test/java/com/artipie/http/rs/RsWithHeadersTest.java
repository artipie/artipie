/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasHeaders;
import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link RsWithHeaders}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class RsWithHeadersTest {

    @Test
    void testRsWithHeadersMapEntry() {
        final String name = "Content-Type";
        final String value = "text/plain; charset=us-ascii";
        MatcherAssert.assertThat(
            new RsWithHeaders(new RsWithStatus(RsStatus.OK), new MapEntry<>(name, value)),
            new RsHasHeaders(new Header(name, value))
        );
    }

    @Test
    void doesNotFilterDuplicatedHeaders() {
        final String name = "Duplicated header";
        final String one = "one";
        final String two = "two";
        MatcherAssert.assertThat(
            new RsWithHeaders(
                new RsFull(RsStatus.OK, new Headers.From(name, one), Content.EMPTY),
                new MapEntry<>(name, two)
            ),
            new RsHasHeaders(
                new Header(name, one), new Header(name, two), new Header("Content-Length", "0")
            )
        );
    }

    @Test
    void filtersDuplicatedHeaders() {
        final String name = "Duplicated header";
        final String one = "one";
        final String two = "two";
        MatcherAssert.assertThat(
            new RsWithHeaders(
                new RsFull(RsStatus.OK, new Headers.From(name, one), Content.EMPTY),
                new Headers.From(name, two), true
            ),
            new RsHasHeaders(new Header(name, two), new Header("Content-Length", "0"))
        );
    }
}
