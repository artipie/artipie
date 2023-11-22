/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.auth;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OAuthTokenFormat}.
 *
 * @since 0.5
 */
class OAuthTokenFormatTest {

    @Test
    void shouldReadToken() {
        MatcherAssert.assertThat(
            new OAuthTokenFormat().token(
                String.join(
                    "\n",
                    "{",
                    "\"access_token\":\"mF_9.B5f-4.1JqM\",",
                    "\"token_type\":\"Bearer\",",
                    "\"expires_in\":3600,",
                    "\"refresh_token\":\"tGzv3JOkF0XG5Qx2TlKWIA\"",
                    "}"
                ).getBytes()
            ),
            new IsEqual<>("mF_9.B5f-4.1JqM")
        );
    }
}
