/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.http.auth.ArtipieAuthFactory;
import com.artipie.http.auth.AuthFactory;
import com.artipie.http.auth.Authentication;
import com.artipie.settings.YamlSettings;

/**
 * Factory for auth from environment.
 * @since 0.30
 */
@ArtipieAuthFactory("artipie")
public final class AuthFromStorageFactory implements AuthFactory {

    @Override
    public Authentication getAuthentication(final YamlMapping yaml) {
        return new YamlSettings.PolicyStorage(yaml).parse().map(
            asto -> new AuthFromStorage(new BlockingStorage(asto))
        ).orElseThrow(
            () ->  new ArtipieException(
                "Failed to create artipie auth, storage is not configured"
            )
        );
    }
}
