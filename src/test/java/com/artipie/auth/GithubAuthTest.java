/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
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
            new IsEqual<>(new Authentication.User("user"))
        );
    }
}
