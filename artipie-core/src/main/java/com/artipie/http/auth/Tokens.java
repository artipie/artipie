/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

/**
 * Authentication tokens: generate token and provide authentication mechanism.
 * @since 1.2
 */
public interface Tokens {

    /**
     * Provide authentication mechanism.
     * @return Implementation of {@link TokenAuthentication}
     */
    TokenAuthentication auth();

    /**
     * Generate token for provided user.
     * @param user User to issue token for
     * @return String token
     */
    String generate(AuthUser user);
}
