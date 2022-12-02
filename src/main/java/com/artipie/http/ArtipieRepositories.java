/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.SliceFromConfig;
import com.artipie.asto.Key;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import com.artipie.settings.ConfigFile;
import com.artipie.settings.Settings;
import com.artipie.settings.StorageAliases;
import com.artipie.settings.cache.StoragesCache;
import com.artipie.settings.repo.RepositoriesFromStorage;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Artipie repositories implementation.
 * @since 0.9
 */
public final class ArtipieRepositories {

    /**
     * HTTP client.
     */
    private final ClientSlices http;

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Storages cache.
     */
    private final StoragesCache cache;

    /**
     * New Artipie repositories.
     * @param http HTTP client
     * @param settings Artipie settings
     * @param cache Storages cache.
     */
    public ArtipieRepositories(
        final ClientSlices http,
        final Settings settings,
        final StoragesCache cache
    ) {
        this.http = http;
        this.settings = settings;
        this.cache = cache;
    }

    /**
     * Find slice by name.
     * @param name Repository name
     * @param port Repository port
     * @return Repository slice
     */
    public Slice slice(final Key name, final int port) {
        return new AsyncSlice(
            new ConfigFile(name).existsIn(this.settings.repoConfigsStorage()).thenCompose(
                exists -> {
                    final CompletionStage<Slice> res;
                    if (exists) {
                        res = this.resolve(name, port);
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
     * @param name Repository name
     * @param port Repository port
     * @return Async slice for repo
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    private CompletionStage<Slice> resolve(final Key name, final int port) {
        return new RepositoriesFromStorage(this.settings, this.cache)
            .config(name.string())
            .thenCombine(
                StorageAliases.find(this.settings.repoConfigsStorage(), name),
                (config, aliases) -> {
                    final Slice res;
                    if (config.port().isEmpty() || config.port().getAsInt() == port) {
                        res = new SliceFromConfig(
                            this.http,
                            this.settings,
                            config,
                            aliases,
                            config.port().isPresent()
                        );
                    } else {
                        res = new SliceSimple(new RsRepoNotFound(name));
                    }
                    return res;
                }
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
