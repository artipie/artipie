/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.misc.ContentAsYaml;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Find aliases settings for repository.
 * @since 0.28
 */
public final class AliasSettings {

    /**
     * Name of the file with storage aliases.
     */
    public static final String FILE_NAME = "_storages.yaml";

    /**
     * Find alias settings for repository.
     *
     * @param storage Settings storage
     * @param repo Repository name
     * @return Instance of {@link StorageByAlias}
     */
    public CompletableFuture<StorageByAlias> find(final Storage storage, final Key repo) {
        final Key.From key = new Key.From(repo, AliasSettings.FILE_NAME);
        return new ConfigFile(key).existsIn(storage).thenCompose(
            found -> {
                final CompletionStage<StorageByAlias> res;
                if (found) {
                    res = SingleInterop.fromFuture(new ConfigFile(key).valueFrom(storage))
                        .to(new ContentAsYaml())
                        .to(SingleInterop.get())
                        .thenApply(StorageByAlias::new);
                } else {
                    res = repo.parent().map(parent -> this.find(storage, parent))
                        .orElse(
                            CompletableFuture.completedFuture(
                                new StorageByAlias(Yaml.createYamlMappingBuilder().build())
                            )
                        );
                }
                return res;
            }
        ).toCompletableFuture();
    }
}
