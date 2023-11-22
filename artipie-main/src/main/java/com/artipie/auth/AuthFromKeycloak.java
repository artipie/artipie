/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.jcabi.log.Logger;
import java.util.Optional;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;

/**
 * Authentication based on keycloak.
 * @since 0.28.0
 */
public final class AuthFromKeycloak implements Authentication {
    /**
     * Configuration.
     */
    private final Configuration config;

    /**
     * Ctor.
     * @param config Configuration
     */
    public AuthFromKeycloak(final Configuration config) {
        this.config = config;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public Optional<AuthUser> user(final String username, final String password) {
        final AuthzClient client = AuthzClient.create(this.config);
        Optional<AuthUser> res;
        try {
            client.authorization(username, password, "openid")
                .authorize(new AuthorizationRequest());
            res = Optional.of(new AuthUser(username, "keycloak"));
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Throwable err) {
            Logger.error(this, err.getMessage());
            res = Optional.empty();
        }
        return res;
    }

    @Override
    public String toString() {
        return String.format("%s()", this.getClass().getSimpleName());
    }
}
