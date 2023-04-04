/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.http.auth.ArtipieAuthFactory;
import com.artipie.http.auth.AuthFactory;
import com.artipie.http.auth.Authentication;
import java.util.Map;
import org.keycloak.authorization.client.Configuration;

/**
 * Factory for auth from keycloak.
 * @since 0.30
 */
@ArtipieAuthFactory("keycloak")
public final class AuthFromKeycloakFactory implements AuthFactory {

    @Override
    public Authentication getAuthentication(final YamlMapping cfg) {
        final YamlMapping creds = cfg.yamlSequence("credentials")
            .values().stream().map(YamlNode::asMapping)
            .filter(node -> "keycloak".equals(node.string("type")))
            .findFirst().orElseThrow();
        return new AuthFromKeycloak(
            new Configuration(
                creds.string("url"),
                creds.string("realm"),
                creds.string("client-id"),
                Map.of("secret", creds.string("client-password")),
                null
            )
        );
    }
}
