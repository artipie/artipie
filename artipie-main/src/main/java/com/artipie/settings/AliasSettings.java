/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
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
     * Settings storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Settings storage
     */
    public AliasSettings(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Find alias settings for repository.
     *
     * @param repo Repository name
     * @return Instance of {@link StorageByAlias}
     */
    public CompletableFuture<StorageByAlias> find(final Key repo) {
        final Key.From key = new Key.From(repo, AliasSettings.FILE_NAME);
        return new ConfigFile(key).existsIn(this.storage).thenCompose(
            found -> {
                final CompletionStage<StorageByAlias> res;
                if (found) {
                    res = SingleInterop.fromFuture(new ConfigFile(key).valueFrom(this.storage))
                        .to(new ContentAsYaml())
                        .to(SingleInterop.get())
                        .thenApply(StorageByAlias::new);
                } else {
                    res = repo.parent().map(this::find)
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
