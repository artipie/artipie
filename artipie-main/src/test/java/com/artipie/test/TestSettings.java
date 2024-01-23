/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.test;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.api.ssl.KeyStore;
import com.artipie.api.ssl.KeyStoreFactory;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.auth.AuthFromEnv;
import com.artipie.http.auth.Authentication;
import com.artipie.scheduling.MetadataEventQueues;
import com.artipie.security.policy.Policy;
import com.artipie.settings.ArtipieSecurity;
import com.artipie.settings.MetricsContext;
import com.artipie.settings.Settings;
import com.artipie.settings.cache.ArtipieCaches;
import java.util.Optional;

/**
 * Test {@link Settings} implementation.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class TestSettings implements Settings {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Yaml `meta` mapping.
     */
    private final YamlMapping meta;

    /**
     * Test caches.
     */
    private final ArtipieCaches caches;

    /**
     * Ctor.
     */
    public TestSettings() {
        this(new InMemoryStorage());
    }

    /**
     * Ctor.
     *
     * @param storage Storage
     */
    public TestSettings(final Storage storage) {
        this(
            storage,
            Yaml.createYamlMappingBuilder().build()
        );
    }

    /**
     * Ctor.
     *
     * @param meta Yaml `meta` mapping
     */
    public TestSettings(final YamlMapping meta) {
        this(new InMemoryStorage(), meta);
    }

    /**
     * Primary ctor.
     *
     * @param storage Storage
     * @param meta Yaml `meta` mapping
         */
    public TestSettings(
        final Storage storage,
        final YamlMapping meta
    ) {
        this.storage = storage;
        this.meta = meta;
        this.caches = new TestArtipieCaches();
    }

    @Override
    public Storage configStorage() {
        return this.storage;
    }

    @Override
    public ArtipieSecurity authz() {
        return new ArtipieSecurity() {
            @Override
            public Authentication authentication() {
                return new AuthFromEnv();
            }

            @Override
            public Policy<?> policy() {
                return Policy.FREE;
            }

            @Override
            public Optional<Storage> policyStorage() {
                return Optional.empty();
            }
        };
    }

    @Override
    public YamlMapping meta() {
        return this.meta;
    }

    @Override
    public Storage repoConfigsStorage() {
        return this.storage;
    }

    @Override
    public Optional<KeyStore> keyStore() {
        return Optional.ofNullable(this.meta().yamlMapping("ssl"))
            .map(KeyStoreFactory::newInstance);
    }

    @Override
    public MetricsContext metrics() {
        return new MetricsContext(Yaml.createYamlMappingBuilder().build());
    }

    @Override
    public ArtipieCaches caches() {
        return this.caches;
    }

    @Override
    public Optional<MetadataEventQueues> artifactMetadata() {
        return Optional.empty();
    }

    @Override
    public Optional<YamlSequence> crontab() {
        return Optional.empty();
    }
}
