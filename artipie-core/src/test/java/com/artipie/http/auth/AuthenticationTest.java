/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link Authentication}.
 *
 * @since 0.15
 */
final class AuthenticationTest {

    @Test
    void wrapDelegatesToOrigin() {
        final String user = "user";
        final String pass = "pass";
        final Optional<AuthUser> result = Optional.of(new AuthUser("result"));
        MatcherAssert.assertThat(
            "Result is forwarded from delegate without modification",
            new TestAuthentication(
                (username, password) -> {
                    MatcherAssert.assertThat(
                        "Username is forwarded to delegate without modification",
                        username,
                        new IsEqual<>(user)
                    );
                    MatcherAssert.assertThat(
                        "Password is forwarded to delegate without modification",
                        password,
                        new IsEqual<>(pass)
                    );
                    return result;
                }
            ).user(user, pass),
            new IsEqual<>(result)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "Aladdin,OpenSesame,true",
        "Aladdin,qwerty,false",
        "JohnDoe,12345,false"
    })
    void singleAuthenticatesAsExpected(
        final String username,
        final String password,
        final boolean expected
    ) {
        MatcherAssert.assertThat(
            new Authentication.Single("Aladdin", "OpenSesame")
                .user(username, password)
                .isPresent(),
            new IsEqual<>(expected)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "Alice,LetMeIn,Alice",
        "Bob,iamgod,Bob"
    })
    void joinedAuthenticatesAsExpected(
        final String username,
        final String password,
        final String expected
    ) {
        MatcherAssert.assertThat(
            new Authentication.Joined(
                new Authentication.Single("Alice", "LetMeIn"),
                new Authentication.Single("Bob", "iamgod")
            ).user(username, password),
            new IsEqual<>(Optional.of(new AuthUser(expected)))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Alice", "Bob", "Jeff"})
    void userHasProperToString(final String username) {
        MatcherAssert.assertThat(
            new AuthUser(username),
            Matchers.hasToString(Matchers.containsString(username))
        );
    }

    @Test
    void emptyOptionalIfNotPresent() {
        MatcherAssert.assertThat(
            new Authentication.Joined(
                new Authentication.Single("Alan", "123"),
                new Authentication.Single("Mark", "0000")
            ).user("Smith", "abc"),
            new IsEqual<>(Optional.empty())
        );
    }

    /**
     * Authentication for testing Authentication.Wrap.
     *
     * @since 0.15
     */
    private static class TestAuthentication extends Authentication.Wrap {

        /**
         * Ctor.
         *
         * @param auth Origin authentication.
         */
        protected TestAuthentication(final Authentication auth) {
            super(auth);
        }
    }

}
