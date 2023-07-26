/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.artipie.settings.Settings;
import com.artipie.settings.repo.RepositoriesFromStorage;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Context class for running scripts. Holds required Artipie objects.
 * @since 0.30
 */
public final class ScriptContext {

    /**
     * Precompiled scripts instances cache.
     */
    private final LoadingCache<FilesContent, Script.PrecompiledScript> scripts;

    /**
     * Repositories info API, available in scripts.
     */
    private final RepositoriesFromStorage repositories;

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
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public ScriptContext(
        final RepositoriesFromStorage repositories, final BlockingStorage storage,
        final Settings settings
    ) {
        this.repositories = repositories;
        this.storage = storage;
        this.settings = settings;
        this.scripts = ScriptContext.createCache();
    }

    /**
     * Getter for precompiled scripts instances cache.
     * @return LoadingCache<> object.
     */
    LoadingCache<FilesContent, Script.PrecompiledScript> getScripts() {
        return this.scripts;
    }

    /**
     * Getter for repositories info API, available in scripts.
     * @return RepositoriesFromStorage object.
     */
    RepositoriesFromStorage getRepositories() {
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
     * @return LoadingCache<> instance for scripts.
     */
    static LoadingCache<FilesContent, Script.PrecompiledScript> createCache() {
        final long duration = new Property(ArtipieProperties.SCRIPTS_TIMEOUT)
            .asLongOrDefault(120_000L);
        return CacheBuilder.newBuilder()
            .expireAfterWrite(duration, TimeUnit.MILLISECONDS)
            .softValues()
            .build(
                new CacheLoader<>() {
                    @Override
                    public Script.PrecompiledScript load(final FilesContent ctx) {
                        return ctx.precompiledScript();
                    }
                }
            );
    }

    /**
     * Extra class for caching precompiled scripts.
     * @since 0.1
     */
    static final class FilesContent {
        /**
         * Key.
         */
        private final Key key;

        /**
         * Storage.
         */
        private final BlockingStorage storage;

        /**
         * Ctor.
         * @param key Key
         * @param storage Storage
         */
        FilesContent(final Key key, final BlockingStorage storage) {
            this.key = key;
            this.storage = storage;
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            final boolean res;
            if (obj == this) {
                res = true;
            } else if (obj instanceof ScriptContext.FilesContent) {
                final ScriptContext.FilesContent data = (ScriptContext.FilesContent) obj;
                res = Objects.equals(this.key, data.key);
            } else {
                res = false;
            }
            return res;
        }

        /**
         * Returns precompiled script for stored key & storage.
         * @return Returns precompiled script object instance.
         */
        public Script.PrecompiledScript precompiledScript() {
            final String ext = FilesContent.extension(this.key.string());
            final Script.ScriptType type = Arrays.stream(Script.ScriptType.values())
                .filter(val -> val.ext().equals(ext)).findFirst().orElse(Script.ScriptType.NONE);
            Script.PrecompiledScript result = null;
            if (!type.equals(Script.ScriptType.NONE)) {
                final String script = new String(this.storage.value(this.key));
                result = new Script.PrecompiledScript(type, script);
            }
            return result;
        }

        /**
         * Obtain extension of filename.
         * @param filename Name of file.
         * @return Extension.
         */
        private static String extension(final String filename) {
            final int pos = filename.lastIndexOf('.');
            final String res;
            if (pos >= 0) {
                res = filename.substring(pos + 1);
            } else {
                res = "";
            }
            return res;
        }
    }
}
