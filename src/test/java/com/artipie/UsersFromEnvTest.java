/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.auth.AuthFromEnv;
import com.artipie.management.Users;
import java.util.Optional;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link UsersFromEnv}.
 * @since 0.10
 */
class UsersFromEnvTest {

    @Test
    void returnsUserFromEnv() {
        final String user = "john";
        MatcherAssert.assertThat(
            new UsersFromEnv(new MapOf<>(new MapEntry<>(AuthFromEnv.ENV_NAME, user)))
                .list().toCompletableFuture().join(),
            Matchers.containsInAnyOrder(new Users.User(user, Optional.empty()))
        );
    }

}
