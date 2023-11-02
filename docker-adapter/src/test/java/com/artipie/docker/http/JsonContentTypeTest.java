/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link JsonContentType}.
 *
 * @since 0.9
 */
public final class JsonContentTypeTest {

    @Test
    void shouldHaveExpectedValue() {
        MatcherAssert.assertThat(
            new JsonContentType().getValue(),
            new IsEqual<>("application/json; charset=utf-8")
        );
    }
}
