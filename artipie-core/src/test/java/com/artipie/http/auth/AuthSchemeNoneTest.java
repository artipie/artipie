/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.http.Headers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AuthScheme#NONE}.
 *
 * @since 0.18
 */
final class AuthSchemeNoneTest {

    @Test
    void shouldAuthEmptyHeadersAsAnonymous() {
        Assertions.assertTrue(
            AuthScheme.NONE.authenticate(Headers.EMPTY)
            .toCompletableFuture().join()
            .user().isAnonymous()
        );
    }
}
