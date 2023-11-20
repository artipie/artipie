/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Authorization.Basic}.
 *
 * @since 0.12
 */
public final class AuthorizationBasicTest {

    @Test
    void shouldHaveExpectedValue() {
        MatcherAssert.assertThat(
            new Authorization.Basic("Aladdin", "open sesame").getValue(),
            new IsEqual<>("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==")
        );
    }

    @Test
    void shouldHaveExpectedCredentials() {
        final String credentials = "123.abc";
        MatcherAssert.assertThat(
            new Authorization.Basic(credentials).credentials(),
            new IsEqual<>(credentials)
        );
    }

    @Test
    void shouldHaveExpectedUsername() {
        MatcherAssert.assertThat(
            new Authorization.Basic("YWxpY2U6b3BlbiBzZXNhbWU=").username(),
            new IsEqual<>("alice")
        );
    }

    @Test
    void shouldHaveExpectedPassword() {
        MatcherAssert.assertThat(
            new Authorization.Basic("QWxhZGRpbjpxd2VydHk=").password(),
            new IsEqual<>("qwerty")
        );
    }
}
