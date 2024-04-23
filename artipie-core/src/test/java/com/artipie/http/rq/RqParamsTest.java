/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rq;

import org.apache.http.client.utils.URIBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Tests for {@link RqParams}.
 */
class RqParamsTest {

    @Test
    void parseUriTest() throws URISyntaxException {
        URI uri = new URIBuilder("http://www.example.com/something.html?one=1&two=2&three=3&three=3a")
            .build();
        RqParams params = new RqParams(uri);
        MatcherAssert.assertThat(params.value("one"),
            Matchers.is(Optional.of("1"))
        );
        MatcherAssert.assertThat(params.value("two"),
            Matchers.is(Optional.of("2"))
        );
        MatcherAssert.assertThat(params.values("three"),
            Matchers.hasItems("3", "3a")
        );
        MatcherAssert.assertThat(params.value("four"),
            Matchers.is(Optional.empty())
        );
        MatcherAssert.assertThat(params.values("four").isEmpty(),
            Matchers.is(true)
        );
    }
}
