/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs.common;

import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.rs.CachedResponse;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link RsJson}.
 *
 * @since 0.16
 */
final class RsTextTest {

    @Test
    void bodyIsCorrect() {
        final String src = "hello";
        MatcherAssert.assertThat(
            new CachedResponse(new RsText(src, StandardCharsets.UTF_16)),
            new RsHasBody(src, StandardCharsets.UTF_16)
        );
    }

    @Test
    void headersHasContentSize() {
        MatcherAssert.assertThat(
            new CachedResponse(new RsText("four")),
            new RsHasHeaders(
                Matchers.equalTo(new Header("Content-Length", "4")),
                Matchers.anything()
            )
        );
    }

    @Test
    void headersHasContentType() {
        MatcherAssert.assertThat(
            new CachedResponse(new RsText("test", StandardCharsets.UTF_16LE)),
            new RsHasHeaders(
                Matchers.equalTo(
                    new Header("Content-Type", "text/plain; charset=UTF-16LE")
                ),
                Matchers.anything()
            )
        );
    }
}
