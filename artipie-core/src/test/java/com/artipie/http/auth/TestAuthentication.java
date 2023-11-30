/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import java.util.Optional;

public class TestAuthentication implements Authentication {
    @Override
    public Optional<AuthUser> user(final String username, final String password) {
        return Optional.of(Authentication.ANONYMOUS);
    }
}
