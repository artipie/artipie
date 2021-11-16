/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.cache;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Cache for credentials with similar configurations in Artipie settings.
 * @since 0.23
 */
public interface CredsConfigCache {
    /**
     * Finds credentials by specified in settings configuration cache or creates
     * a new item and caches it.
     * @param storage Storage
     * @param path Path to the credentials file
     * @return Yaml credentials
     */
    CompletionStage<YamlMapping> credentials(Storage storage, Key path);

    /**
     * Invalidate all items in cache.
     */
    void invalidateAll();

    /**
     * Fake implementation of {@link CredsConfigCache}.
     * @since 0.23
     */
    class Fake implements CredsConfigCache {
        /**
         * Users credentials.
         */
        private final CompletionStage<YamlMapping> creds;

        /**
         * Ctor.
         */
        public Fake() {
            this.creds = CompletableFuture.completedFuture(Yaml.createYamlMappingBuilder().build());
        }

        @Override
        public CompletionStage<YamlMapping> credentials(final Storage storage, final Key path) {
            return this.creds;
        }

        @Override
        public void invalidateAll() {
            // do nothing
        }
    }
}
