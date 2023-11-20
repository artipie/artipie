/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.api.ssl.KeyStore;
import com.artipie.asto.Storage;
import com.artipie.scheduling.MetadataEventQueues;
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
     * Artipie authorization.
     * @return Authentication and policy
     */
    ArtipieSecurity authz();

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

    /**
     * Artifact metadata events queue.
     * @return Artifact events queue
     */
    Optional<MetadataEventQueues> artifactMetadata();

    /**
     * Crontab settings.
     * @return Yaml sequence of crontab strings.
     */
    Optional<YamlSequence> crontab();
}
