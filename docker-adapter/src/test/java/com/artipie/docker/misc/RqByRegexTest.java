/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.misc;

import java.util.regex.Pattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RqByRegex}.
 * @since 0.3
 */
class RqByRegexTest {

    @Test
    void shouldMatchPath() {
        MatcherAssert.assertThat(
            new RqByRegex("GET /v2/some/repo HTTP/1.1", Pattern.compile("/v2/.*")).path().matches(),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldThrowExceptionIsDoesNotMatch() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new RqByRegex("GET /v3/my-repo/blobs HTTP/1.1", Pattern.compile("/v2/.*/blobs"))
                .path()
        );
    }

}
