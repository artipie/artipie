/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.ArtipieException;
import com.artipie.http.auth.Authentication;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.keycloak.TokenVerifier;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;

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
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Optional<User> user(final String username, final String password) {
        final AuthzClient client = AuthzClient.create(this.config);
        try {
            final AuthorizationResponse response = client
                .authorization(username, password, "openid")
                .authorize(new AuthorizationRequest());
            final AccessToken token = TokenVerifier.create(response.getToken(), AccessToken.class)
                .getToken();
            final Set<String> roles = new HashSet<>();
            roles.addAll(AuthFromKeycloak.realmRoles(token));
            roles.addAll(AuthFromKeycloak.clientRoles(token));
            return Optional.of(new User(username, roles.stream().toList()));
        } catch (final VerificationException exc) {
            throw new ArtipieException(exc);
        }
    }

    @Override
    public String toString() {
        return String.format("%s()", this.getClass().getSimpleName());
    }

    /**
     * Retrieves realm roles.
     * @param token AccessToken
     * @return Realm roles.
     */
    private static Set<String> realmRoles(final AccessToken token) {
        return token.getRealmAccess().getRoles();
    }

    /**
     * Retrieves client application roles.
     * @param token AccessToken
     * @return Client application roles.
     */
    private static Set<String> clientRoles(final AccessToken token) {
        final Set<String> roles = new HashSet<>();
        token.getResourceAccess().forEach((k, v) -> roles.addAll(v.getRoles()));
        return roles;
    }
}
