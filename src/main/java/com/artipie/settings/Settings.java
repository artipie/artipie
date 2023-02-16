/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.api.ssl.KeyStore;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.auth.Authentication;
import com.artipie.settings.cache.ArtipieCaches;
import java.util.Optional;

/**
 * Application settings.
 *
 * @since 0.1
 */
public interface Settings {

    /**
     * Provides a configuration storage.
     *
     * @return Storage instance.
     */
    Storage configStorage();

    /**
     * Provides authorization.
     *
     * @return Authentication instance
     */
    Authentication auth();

    /**
     * Repository layout.
     * @return Repository layout
     */
    Layout layout();

    /**
     * Artipie meta configuration.
     * @return Yaml mapping
     */
    YamlMapping meta();

    /**
     * Repo configs storage, or, in file system storage terms, subdirectory where repo
     * configs are located relatively to the storage.
     * @return Repo configs storage
     */
    Storage repoConfigsStorage();

    /**
     * Key for credentials yaml settings.
     * @return Key for credentials
     */
    Optional<Key> credentialsKey();

    /**
     * Key store.
     * @return KeyStore
     */
    Optional<KeyStore> keyStore();

    /**
     * Metrics setting.
     * @return Metrics configuration
     */
    MetricsContext metrics();

    /**
     * Artipie caches.
     * @return The caches
     */
    ArtipieCaches caches();

}
