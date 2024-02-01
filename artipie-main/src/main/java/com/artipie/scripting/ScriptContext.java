/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.artipie.settings.Settings;
import com.artipie.settings.repo.Repositories;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.TimeUnit;

/**
 * Context class for running scripts. Holds required Artipie objects.
 * @since 0.30
 */
public final class ScriptContext {

    /**
     * Precompiled scripts instances cache.
     */
    private final LoadingCache<Key, Script.PrecompiledScript> scripts;

    /**
     * Repositories info API, available in scripts.
     */
    private final Repositories repositories;

    /**
     * Blocking storage instance to access scripts.
     */
    private final BlockingStorage storage;

    /**
     * Settings API, available in scripts.
     */
    private final Settings settings;

    /**
     * Context class for running scripts. Holds required Artipie objects.
     * @param repositories Repositories info API, available in scripts.
     * @param storage Blocking storage instance to access scripts.
     * @param settings Settings API, available in scripts.
     */
    public ScriptContext(
        final Repositories repositories,
        final BlockingStorage storage,
        final Settings settings
    ) {
        this.repositories = repositories;
        this.storage = storage;
        this.settings = settings;
        this.scripts = ScriptContext.createCache(storage);
    }

    /**
     * Getter for precompiled scripts instances cache.
     * @return LoadingCache<> object.
     */
    LoadingCache<Key, Script.PrecompiledScript> getScripts() {
        return this.scripts;
    }

    /**
     * Getter for repositories info API, available in scripts.
     * @return Repositories object.
     */
    Repositories getRepositories() {
        return this.repositories;
    }

    /**
     * Getter for blocking storage instance to access scripts.
     * @return BlockingStorage object.
     */
    BlockingStorage getStorage() {
        return this.storage;
    }

    /**
     * Getter for settings API, available in scripts.
     * @return Settings object.
     */
    Settings getSettings() {
        return this.settings;
    }

    /**
     * Create cache for script objects.
     * @param storage Storage which contains scripts.
     * @return LoadingCache<> instance for scripts.
     */
    static LoadingCache<Key, Script.PrecompiledScript> createCache(final BlockingStorage storage) {
        final long duration = new Property(ArtipieProperties.SCRIPTS_TIMEOUT)
            .asLongOrDefault(120_000L);
        return CacheBuilder.newBuilder()
            .expireAfterWrite(duration, TimeUnit.MILLISECONDS)
            .softValues()
            .build(
                new CacheLoader<>() {
                    @Override
                    public Script.PrecompiledScript load(final Key key) {
                        return new Script.PrecompiledScript(key, storage);
                    }
                }
            );
    }
}
