/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs.common;

import com.artipie.http.headers.Header;
import com.artipie.http.hm.IsJson;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.rs.CachedResponse;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Test case for {@link RsJson}.
 *
 * @since 0.16
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class RsJsonTest {

    @Test
    void bodyIsCorrect() {
        MatcherAssert.assertThat(
            new RsJson(Json.createObjectBuilder().add("foo", true)),
            new RsHasBody("{\"foo\":true}", StandardCharsets.UTF_8)
        );
    }

    @Test
    void headersHasContentSize() {
        MatcherAssert.assertThat(
            new CachedResponse(new RsJson(Json.createObjectBuilder().add("bar", 0))),
            new RsHasHeaders(
                Matchers.equalTo(new Header("Content-Length", "9")),
                Matchers.anything()
            )
        );
    }

    @Test
    void bodyMatchesJson() {
        final String field = "faz";
        MatcherAssert.assertThat(
            new RsJson(Json.createObjectBuilder().add(field, true)),
            new RsHasBody(
                new IsJson(
                    new JsonHas(
                        field,
                        new JsonValueIs(true)
                    )
                )
            )
        );
    }

    @Test
    void headersHasContentType() {
        MatcherAssert.assertThat(
            new CachedResponse(
                new RsJson(
                    () -> Json.createObjectBuilder().add("baz", "a").build(),
                    StandardCharsets.UTF_16BE
                )
            ),
            new RsHasHeaders(
                Matchers.equalTo(
                    new Header("Content-Type", "application/json; charset=UTF-16BE")
                ),
                Matchers.anything()
            )
        );
    }

}
