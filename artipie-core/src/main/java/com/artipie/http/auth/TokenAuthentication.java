/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Mechanism to authenticate user by token.
 *
 * @since 0.17
 */
public interface TokenAuthentication {

    /**
     * Authenticate user by token.
     *
     * @param token Token.
     * @return User if authenticated.
     */
    CompletionStage<Optional<AuthUser>> user(String token);
}
