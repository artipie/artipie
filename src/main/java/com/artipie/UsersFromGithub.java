/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.auth.GithubAuth;
import com.artipie.auth.Users;
import com.artipie.http.auth.Authentication;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Credentials from GitHub.
 * @since 0.12.2
 */
public final class UsersFromGithub implements Users {
    @Override
    public CompletionStage<List<User>> list() {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Listing users is not supported")
        );
    }

    @Override
    public CompletionStage<Void> add(
        final User user,
        final String pswd,
        final PasswordFormat format
    ) {
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
        return CompletableFuture.completedFuture(new GithubAuth());
    }
}
