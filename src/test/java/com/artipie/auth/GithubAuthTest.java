/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.ArtipieException;
import com.artipie.http.auth.AuthUser;
import java.util.Optional;
import org.cactoos.text.Joined;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link GithubAuth}.
 *
 * @since 0.10
 */
final class GithubAuthTest {

    @Test
    void resolveUserByToken() {
        final String secret = "secret";
        MatcherAssert.assertThat(
            new GithubAuth(
                // @checkstyle ReturnCountCheck (5 lines)
                token -> {
                    if (token.equals(secret)) {
                        return "User";
                    }
                    return "";
                }
            ).user("github.com/UsEr", secret).orElseThrow(),
            new IsEqual<>(new AuthUser("UsEr", "test"))
        );
    }

    @Test
    void shouldReturnOptionalEmptyWhenRequestIsUnauthorized() {
        MatcherAssert.assertThat(
            new GithubAuth(
                token -> {
                    throw new AssertionError(
                        new Joined(
                            "HTTP response status is not equal to 200:\n",
                            "401 Unauthorized [https://api.github.com/user]"
                        )
                    );
                }
            ).user("github.com/bad_user", "bad_secret"),
            new IsEqual<>(Optional.empty())
        );
    }

    @Test
    void shouldThrownExceptionWhenAssertionErrorIsHappened() {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new GithubAuth(
                token -> {
                    throw new AssertionError("Any error");
                }
            ).user("github.com/user", "pwd")
        );
    }
}
