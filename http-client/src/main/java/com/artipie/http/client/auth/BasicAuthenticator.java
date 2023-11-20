/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.auth;

import com.artipie.http.Headers;
import com.artipie.http.headers.Authorization;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Basic authenticator for given username and password.
 *
 * @since 0.3
 */
public final class BasicAuthenticator implements Authenticator {

    /**
     * Username.
     */
    private final String username;

    /**
     * Password.
     */
    private final String password;

    /**
     * Ctor.
     *
     * @param username Username.
     * @param password Password.
     */
    public BasicAuthenticator(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public CompletionStage<Headers> authenticate(final Headers headers) {
        return CompletableFuture.completedFuture(
            new Headers.From(new Authorization.Basic(this.username, this.password))
        );
    }
}
