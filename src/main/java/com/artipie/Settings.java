/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.Authentication;
import java.io.IOException;
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
     * @throws IOException In case of problems with reading settings.
     */
    Storage storage() throws IOException;

    /**
     * Provides authorization.
     *
     * @return Authentication instance
     */
    CompletionStage<Authentication> auth();

    /**
     * Repository layout.
     * @return Repository layout
     * @throws IOException If failet to parse settings
     */
    String layout() throws IOException;

    /**
     * Artipie meta configuration.
     * @return Yaml mapping
     * @throws IOException On error
     */
    YamlMapping meta() throws IOException;

    /**
     * Artipie credentials.
     * @return Completion action with credentials
     */
    CompletionStage<Credentials> credentials();

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
        private final Credentials cred;

        /**
         * Yaml `meta` mapping.
         */
        private final YamlMapping meta;

        /**
         * Ctor.
         */
        public Fake() {
            this(
                new InMemoryStorage(), new Credentials.FromEnv(),
                Yaml.createYamlMappingBuilder().build()
            );
        }

        /**
         * Ctor.
         *
         * @param storage Storage
         */
        public Fake(final Storage storage) {
            this(storage, new Credentials.FromEnv(), Yaml.createYamlMappingBuilder().build());
        }

        /**
         * Ctor.
         *
         * @param cred Credentials
         */
        public Fake(final Credentials cred) {
            this(new InMemoryStorage(), cred, Yaml.createYamlMappingBuilder().build());
        }

        /**
         * Ctor.
         *
         * @param cred Credentials
         * @param meta Yaml `meta` mapping
         */
        public Fake(final Credentials cred, final YamlMapping meta) {
            this(new InMemoryStorage(), cred, meta);
        }

        /**
         * Primary ctor.
         *  @param storage Storage
         * @param cred Credentials
         * @param meta Yaml `meta` mapping
         */
        public Fake(final Storage storage, final Credentials cred, final YamlMapping meta) {
            this.storage = storage;
            this.cred = cred;
            this.meta = meta;
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
        public String layout() {
            return "flat";
        }

        @Override
        public YamlMapping meta() {
            return this.meta;
        }

        @Override
        public CompletionStage<Credentials> credentials() {
            return CompletableFuture.completedFuture(this.cred);
        }
    }
}
