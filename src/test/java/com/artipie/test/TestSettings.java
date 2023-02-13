/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.test;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.api.ssl.KeyStore;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.Authentication;
import com.artipie.settings.Layout;
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
     * Layout.
     */
    private final Layout layout;

    /**
     * KeyStore.
     */
    private final Optional<KeyStore> keystore;

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
     * @param layout Layout
     */
    public TestSettings(final Layout layout) {
        this(new InMemoryStorage(), layout);
    }

    /**
     * Ctor.
     *
     * @param storage Storage
     * @param layout Layout
     */
    public TestSettings(final Storage storage, final Layout layout) {
        this(
            storage,
            Yaml.createYamlMappingBuilder().build(),
            layout,
            Optional.empty()
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
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public TestSettings(final Storage storage, final YamlMapping meta) {
        this(storage, meta, new Layout.Flat(), Optional.empty());
    }

    /**
     * Primary ctor.
     *
     * @param storage Storage
     * @param meta Yaml `meta` mapping
     * @param layout Layout
     * @param keystore KeyStore
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public TestSettings(
        final Storage storage,
        final YamlMapping meta,
        final Layout layout,
        final Optional<KeyStore> keystore
    ) {
        this.storage = storage;
        this.meta = meta;
        this.layout = layout;
        this.keystore = keystore;
        this.caches = new TestArtipieCaches();
    }

    @Override
    public Storage configStorage() {
        return this.storage;
    }

    @Override
    public Authentication auth() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Layout layout() {
        return this.layout;
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
    public Optional<Key> credentialsKey() {
        return Optional.empty();
    }

    @Override
    public Optional<KeyStore> keyStore() {
        return this.keystore;
    }

    @Override
    public MetricsContext metrics() {
        return new MetricsContext(Yaml.createYamlMappingBuilder().build());
    }

    @Override
    public ArtipieCaches caches() {
        return this.caches;
    }
}
