/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlInput;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.api.RepositoryName;
import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.settings.cache.StoragesCache;
import com.jcabi.log.Logger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Repository data management.
 * @since 0.1
 */
public final class RepoData {
    /**
     * Key 'storage' inside json-object.
     */
    private static final String STORAGE = "storage";

    /**
     * Repository settings storage.
     */
    private final Storage configStorage;

    /**
     * Storages cache.
     */
    private final StoragesCache storagesCache;

    /**
     * Ctor.
     *
     * @param configStorage Repository settings storage
     * @param storagesCache Storages cache
     */
    public RepoData(final Storage configStorage, final StoragesCache storagesCache) {
        this.configStorage = configStorage;
        this.storagesCache = storagesCache;
    }

    /**
     * Remove data from the repository.
     * @param rname Repository name
     * @return Completable action of the remove operation
     */
    public CompletionStage<Void> remove(final RepositoryName rname) {
        final String repo = rname.toString();
        return this.repoStorage(rname)
            .thenAccept(
                asto ->
                    asto
                        .deleteAll(new Key.From(repo))
                        .thenAccept(
                            nothing ->
                                Logger.info(
                                    this,
                                    String.format("Removed data from repository %s", repo)
                                )
                        )
            );
    }

    /**
     * Move data when repository is renamed: from location by the old name to location with
     * new name.
     * @param rname Repository name
     * @param nname New repository name
     * @return Completable action of the remove operation
     */
    public CompletionStage<Void> move(final RepositoryName rname, final RepositoryName nname) {
        final Key repo = new Key.From(rname.toString());
        final Key nrepo = new Key.From(nname.toString());
        return this.repoStorage(rname)
            .thenCompose(
                asto ->
                    new SubStorage(repo, asto)
                        .list(Key.ROOT)
                        .thenCompose(
                            list ->
                                new Copy(new SubStorage(repo, asto), list)
                                    .copy(new SubStorage(nrepo, asto))
                        ).thenCompose(nothing -> asto.deleteAll(new Key.From(repo)))
                        .thenAccept(
                            nothing ->
                                Logger.info(
                                    this,
                                    String.format(
                                        "Moved data from repository %s to %s",
                                        repo,
                                        nrepo
                                    )
                                )
                        )
            );
    }

    /**
     * Obtain storage from repository settings.
     * @param rname Repository name
     * @return Abstract storage
     */
    private CompletionStage<Storage> repoStorage(final RepositoryName rname) {
        return new ConfigFile(String.format("%s.yaml", rname.toString()))
            .valueFrom(this.configStorage)
            .thenApply(PublisherAs::new)
            .thenCompose(PublisherAs::asciiString)
            .thenApply(Yaml::createYamlInput)
            .thenApply(new UncheckedIOFunc<>(YamlInput::readYamlMapping))
            .thenApply(yaml -> yaml.yamlMapping("repo").value(RepoData.STORAGE))
            .thenCompose(
                node -> {
                    final CompletionStage<Storage> res;
                    if (node instanceof Scalar) {
                        res = new AliasSettings(this.configStorage).find(
                            new Key.From(rname.toString())
                        ).thenApply(
                            aliases -> aliases.storage(
                                this.storagesCache,
                                ((Scalar) node).value()
                            )
                        );
                    } else if (node instanceof YamlMapping) {
                        res = CompletableFuture.completedStage(
                            this.storagesCache.storage((YamlMapping) node)
                        );
                    } else {
                        res = CompletableFuture.failedFuture(
                            new IllegalStateException(
                                String.format("Invalid storage config: %s", node)
                            )
                        );
                    }
                    return res;
                }
            );
    }
}
