/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.api.RepositoryName;
import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.settings.repo.CrudRepoSettings;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * Repository data management.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RepoData {
    /**
     * Repository settings create/read/update/delete.
     */
    private final CrudRepoSettings crs;

    /**
     * Ctor.
     * @param crs Repository settings create/read/update/delete
     */
    public RepoData(final CrudRepoSettings crs) {
        this.crs = crs;
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
        try {
            final YamlMapping yaml = this.crs.valueAsYaml(rname);
            YamlMapping res = yaml.yamlMapping("storage");
            if (res == null) {
                res = this.storageYamlByAlias(rname, yaml.string("storage"));
            }
            return new YamlStorage(res).storage();
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }

    /**
     * Find storage settings by alias, considering two file extensions and two locations.
     * @param rname Repository name
     * @param alias Storage settings yaml by alias
     * @return Yaml storage settings found by provided alias
     * @throws IllegalStateException If storage with given alias not found
     * @throws UncheckedIOException On IO errors
     * @checkstyle LineLengthCheck (2 lines)
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
        ).filter(this.crs.repoConfigsStorage()::exists).findFirst();
        if (location.isPresent()) {
            try {
                res = Optional.of(
                    Yaml.createYamlInput(
                        new String(
                            this.crs.repoConfigsStorage().value(location.get()),
                            StandardCharsets.UTF_8
                        )
                    ).readYamlMapping().yamlMapping("storages").yamlMapping(alias)
                );
            } catch (final IOException err) {
                throw new UncheckedIOException(err);
            }
        }
        return res.orElseThrow(
            () -> new IllegalStateException(String.format("Storage alias %s not found", alias))
        );
    }
}
