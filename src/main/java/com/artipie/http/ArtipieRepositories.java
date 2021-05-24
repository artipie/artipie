/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.RepositoriesFromStorage;
import com.artipie.Settings;
import com.artipie.SliceFromConfig;
import com.artipie.StorageAliases;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import com.artipie.repo.ConfigFile;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Artipie repositories implementation.
 * @since 0.9
 */
public final class ArtipieRepositories {

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * New Artipie repositories.
     * @param settings Artipie settings
     */
    public ArtipieRepositories(final Settings settings) {
        this.settings = settings;
    }

    /**
     * Find slice by name.
     * @param name Repository name
     * @param standalone Standalone flag
     * @return Repository slice
     */
    public Slice slice(final Key name, final boolean standalone) {
        final Storage storage = this.settings.repoConfigsStorage();
        return new AsyncSlice(
            new ConfigFile(name).existsIn(storage).thenCompose(
                exists -> {
                    final CompletionStage<Slice> res;
                    if (exists) {
                        res = this.resolve(storage, name, standalone);
                    } else {
                        res = CompletableFuture.completedFuture(
                            new SliceSimple(new RsRepoNotFound(name))
                        );
                    }
                    return res;
                }
            )
        );
    }

    /**
     * Resolve async {@link Slice} by provided configuration.
     * @param storage Artipie config storage
     * @param name Repository name
     * @param standalone Standalone flag
     * @return Async slice for repo
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    private CompletionStage<Slice> resolve(
        final Storage storage,
        final Key name,
        final boolean standalone
    ) {
        return new RepositoriesFromStorage(storage).config(name.string()).thenCombine(
            StorageAliases.find(storage, name),
            (config, aliases) -> new SliceFromConfig(this.settings, config, aliases, standalone)
        );
    }

    /**
     * Repo not found response.
     * @since 0.9
     */
    private static final class RsRepoNotFound extends Response.Wrap {

        /**
         * New repo not found response.
         * @param repo Repo name
         */
        RsRepoNotFound(final Key repo) {
            super(
                new RsWithBody(
                    StandardRs.NOT_FOUND,
                    String.format("Repository '%s' not found", repo.string()),
                    StandardCharsets.UTF_8
                )
            );
        }
    }
}
