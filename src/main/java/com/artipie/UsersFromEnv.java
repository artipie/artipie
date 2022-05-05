/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.auth.AuthFromEnv;
import com.artipie.auth.Users;
import com.artipie.http.auth.Authentication;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.cactoos.list.ListOf;

/**
 * Credentials from env.
 * @since 0.12.2
 */
public final class UsersFromEnv implements Users {

    /**
     * Environment variables.
     */
    private final Map<String, String> env;

    /**
     * Ctor.
     */
    public UsersFromEnv() {
        this(System.getenv());
    }

    /**
     * Ctor.
     * @param env Environment variables
     */
    public UsersFromEnv(final Map<String, String> env) {
        this.env = env;
    }

    @Override
    public CompletionStage<List<User>> list() {
        return CompletableFuture.completedFuture(
            Optional.ofNullable(this.env.get(AuthFromEnv.ENV_NAME))
                .<List<User>>map(name -> new ListOf<>(new User(name, Optional.empty())))
                .orElse(Collections.emptyList())
        );
    }

    @Override
    public CompletionStage<Void> add(final User user, final String pswd,
        final PasswordFormat format) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Adding users is not supported")
        );
    }

    @Override
    public CompletionStage<Void> remove(final String username) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Removing users is not supported")
        );
    }

    @Override
    public CompletionStage<Authentication> auth() {
        return CompletableFuture.completedFuture(new AuthFromEnv());
    }
}
