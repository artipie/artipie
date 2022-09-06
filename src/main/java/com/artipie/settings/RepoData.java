/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

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
import com.jcabi.log.Logger;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

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
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Repository settings storage.
     */
    public RepoData(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Remove data from the repository.
     * @param rname Repository name
     * @return Completable action of the remove operation
     */
    public CompletionStage<Void> remove(final RepositoryName rname) {
        final String repo = rname.toString();
        return this.asto(rname).deleteAll(new Key.From(repo)).thenAccept(
            nothing -> Logger.info(this, String.format("Removed data from repository %s", repo))
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
        final Storage asto = this.asto(rname);
        return new SubStorage(repo, asto).list(Key.ROOT).thenCompose(
            list -> new Copy(new SubStorage(repo, asto), list)
                .copy(new SubStorage(nrepo, asto))
        ).thenCompose(nothing -> asto.deleteAll(new Key.From(repo))).thenAccept(
            nothing ->
                Logger.info(this, String.format("Moved data from repository %s to %s", repo, nrepo))
        );
    }

    /**
     * Obtain storage from repository settings.
     * @param rname Repository name
     * @return Abstract storage
     * @throws UncheckedIOException On IO errors
     */
    private Storage asto(final RepositoryName rname) {
        return new ConfigFile(String.format("%s.yaml", rname.toString()))
            .valueFrom(this.storage)
            .thenApply(PublisherAs::new)
            .thenCompose(PublisherAs::asciiString)
            .thenApply(Yaml::createYamlInput)
            .thenApply(new UncheckedIOFunc<>(YamlInput::readYamlMapping))
            .thenApply(yaml -> yaml.yamlMapping("repo"))
            .thenApply(
                yaml -> {
                    YamlMapping res = yaml.yamlMapping(RepoData.STORAGE);
                    if (res == null) {
                        res = this.storageYamlByAlias(rname, yaml.string(RepoData.STORAGE));
                    }
                    return res;
                }
            )
            .thenApply(yaml -> new YamlStorage(yaml).storage())
            .toCompletableFuture().join();
    }

    /**
     * Find storage settings by alias, considering two file extensions and two locations.
     * @param rname Repository name
     * @param alias Storage settings yaml by alias
     * @return Yaml storage settings found by provided alias
     * @throws IllegalStateException If storage with given alias not found
     * @throws UncheckedIOException On IO errors
     */
    private YamlMapping storageYamlByAlias(final RepositoryName rname, final String alias) {
        final Key repo = new Key.From(rname.toString());
        final Key yml = new Key.From("_storage.yaml");
        final Key yaml = new Key.From("_storage.yml");
        Optional<YamlMapping> res = Optional.empty();
        final Optional<Key> location = Stream.of(
            new Key.From(repo, yaml), new Key.From(repo, yml),
            repo.parent().<Key>map(item -> new Key.From(item, yaml)).orElse(yaml),
            repo.parent().<Key>map(item -> new Key.From(item, yml)).orElse(yml)
        ).filter(key -> this.storage.exists(key).join()).findFirst();
        if (location.isPresent()) {
            res = Optional.of(
                this.storage.value(location.get())
                .thenApply(PublisherAs::new)
                .thenCompose(PublisherAs::asciiString)
                .thenApply(Yaml::createYamlInput)
                .thenApply(new UncheckedIOFunc<>(YamlInput::readYamlMapping))
                .thenApply(s -> s.yamlMapping("storages").yamlMapping(alias))
                .toCompletableFuture().join()
            );
        }
        return res.orElseThrow(
            () -> new IllegalStateException(String.format("Storage alias %s not found", alias))
        );
    }
}
