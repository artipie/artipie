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
import com.artipie.asto.Concatenation;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.repo.ConfigFile;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Storage configuration by alias.
 * @since 0.4
 */
public interface StorageAliases {

    /**
     * Empty storage alias.
     */
    StorageAliases EMPTY = alias -> {
        throw new IllegalStateException(String.format("No storage alias found: %s", alias));
    };

    /**
     * Name of the file with storage aliases.
     */
    String FILE_NAME = "_storages.yaml";

    /**
     * Find storage by alias.
     * @param alias Storage alias
     * @return Storage instance
     */
    Storage storage(String alias);

    /**
     * Find storage aliases config for repo.
     * @param storage Config storage
     * @param repo Repo key
     * @return Async storages
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static CompletableFuture<StorageAliases> find(final Storage storage, final Key repo) {
        final Key.From key = new Key.From(repo, StorageAliases.FILE_NAME);
        return new ConfigFile(key).existsIn(storage).thenCompose(
            found -> {
                final CompletionStage<StorageAliases> res;
                if (found) {
                    res = new ConfigFile(key).valueFrom(storage).thenCompose(
                        pub -> new Concatenation(pub).single()
                            .map(buf -> new Remaining(buf).bytes())
                            .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                            .map(cnt -> Yaml.createYamlInput(cnt).readYamlMapping())
                            .to(SingleInterop.get())
                            .thenApply(FromYaml::new)
                    );
                } else {
                    res = repo.parent()
                        .map(parent -> StorageAliases.find(storage, parent))
                        .orElse(CompletableFuture.completedFuture(StorageAliases.EMPTY));
                }
                return res;
            }
        ).toCompletableFuture();
    }

    /**
     * Storage aliases from Yaml config.
     * @since 0.4
     */
    final class FromYaml implements StorageAliases {

        /**
         * Aliases yaml.
         */
        private final YamlMapping yaml;

        /**
         * Aliases from yaml.
         * @param yaml Yaml
         */
        public FromYaml(final YamlMapping yaml) {
            this.yaml = yaml;
        }

        @Override
        public Storage storage(final String alias) {
            return Optional.ofNullable(this.yaml.yamlMapping("storages")).map(
                node -> Optional.ofNullable(node.yamlMapping(alias)).map(
                    aliasyaml -> new YamlStorage(aliasyaml).storage()
                ).orElseThrow(FromYaml::illegalState)
            ).orElseThrow(FromYaml::illegalState);
        }

        /**
         * Throws illegal state exception.
         * @return Illegal state exception.
         */
        private static RuntimeException illegalState() {
            throw new IllegalStateException(
                "yaml file with aliases is malformed or alias is absent"
            );
        }
    }
}
