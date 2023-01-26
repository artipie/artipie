/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.users;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.auth.AuthFromKeycloak;
import com.artipie.http.auth.Authentication;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.keycloak.authorization.client.Configuration;

/**
 * Credentials from keycloak.
 *
 * @since 0.28.0
 */
public final class UsersFromKeycloak implements Users {
    /**
     * Configuration.
     */
    private final Configuration config;

    /**
     * Ctor.
     * @param settings Yaml configuration for keycloak.
     */
    public UsersFromKeycloak(final YamlMapping settings) {
        this.config = new Configuration(
            settings.string("url"),
            settings.string("realm"),
            settings.string("client-id"),
            Map.of("secret", settings.string("client-password")),
            null
        );
    }

    @Override
    public CompletionStage<List<User>> list() {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Listing users is not supported")
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
        return CompletableFuture.completedFuture(new AuthFromKeycloak(this.config));
    }
}
