/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.misc.ContentAsYaml;
import com.artipie.repo.ConfigFile;
import hu.akarnokd.rxjava2.interop.SingleInterop;
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
                    res = SingleInterop.fromFuture(
                        new ConfigFile(key).valueFrom(storage)
                    ).to(new ContentAsYaml())
                    .to(SingleInterop.get())
                    .thenApply(FromYaml::new);
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
