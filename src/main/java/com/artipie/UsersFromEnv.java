/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.artipie.auth.AuthFromEnv;
import com.artipie.http.auth.Authentication;
import com.artipie.management.Users;
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
