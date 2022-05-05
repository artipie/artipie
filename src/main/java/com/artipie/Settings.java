/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.auth.Users;
import com.artipie.http.auth.Authentication;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Application settings.
 *
 * @since 0.1
 */
public interface Settings {

    /**
     * Provides a storage.
     *
     * @return Storage instance.
     */
    Storage storage();

    /**
     * Provides authorization.
     *
     * @return Authentication instance
     */
    CompletionStage<Authentication> auth();

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
     * Artipie credentials.
     * @return Completion action with credentials
     */
    CompletionStage<Users> credentials();

    /**
     * Fake {@link Settings} using a file storage.
     *
     * @since 0.2
     */
    final class Fake implements Settings {

        /**
         * Storage.
         */
        private final Storage storage;

        /**
         * Credentials.
         */
        private final Users cred;

        /**
         * Yaml `meta` mapping.
         */
        private final YamlMapping meta;

        /**
         * Layout.
         */
        private final Layout layout;

        /**
         * Ctor.
         */
        public Fake() {
            this(new InMemoryStorage());
        }

        /**
         * Ctor.
         *
         * @param storage Storage
         */
        public Fake(final Storage storage) {
            this(
                storage,
                new UsersFromEnv(),
                Yaml.createYamlMappingBuilder().build()
            );
        }

        /**
         * Ctor.
         *
         * @param layout Layout
         */
        public Fake(final Layout layout) {
            this(new InMemoryStorage(), layout);
        }

        /**
         * Ctor.
         *
         * @param storage Storage
         * @param layout Layout
         */
        public Fake(final Storage storage, final Layout layout) {
            this(
                storage,
                new UsersFromEnv(),
                Yaml.createYamlMappingBuilder().build(),
                layout
            );
        }

        /**
         * Ctor.
         *
         * @param cred Credentials
         */
        public Fake(final Users cred) {
            this(new InMemoryStorage(), cred, Yaml.createYamlMappingBuilder().build());
        }

        /**
         * Ctor.
         *
         * @param cred Credentials
         * @param meta Yaml `meta` mapping
         */
        public Fake(final Users cred, final YamlMapping meta) {
            this(new InMemoryStorage(), cred, meta);
        }

        /**
         * Primary ctor.
         *
         * @param storage Storage
         * @param cred Credentials
         * @param meta Yaml `meta` mapping
         * @checkstyle ParameterNumberCheck (2 lines)
         */
        public Fake(final Storage storage, final Users cred, final YamlMapping meta) {
            this(storage, cred, meta, new Layout.Flat());
        }

        /**
         * Primary ctor.
         *
         * @param storage Storage
         * @param cred Credentials
         * @param meta Yaml `meta` mapping
         * @param layout Layout
         * @checkstyle ParameterNumberCheck (2 lines)
         */
        public Fake(
            final Storage storage,
            final Users cred,
            final YamlMapping meta,
            final Layout layout
        ) {
            this.storage = storage;
            this.cred = cred;
            this.meta = meta;
            this.layout = layout;
        }

        @Override
        public Storage storage() {
            return this.storage;
        }

        @Override
        public CompletionStage<Authentication> auth() {
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
        public CompletionStage<Users> credentials() {
            return CompletableFuture.completedFuture(this.cred);
        }
    }
}
