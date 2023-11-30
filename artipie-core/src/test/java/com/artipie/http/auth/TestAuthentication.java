/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import java.util.Optional;

/**
 * Test Authentication implementation.
 * @since 0.8
 */
public final class TestAuthentication implements Authentication {
    @Override
    public Optional<AuthUser> user(final String username, final String password) {
        return Optional.of(Authentication.ANONYMOUS);
    }
}
