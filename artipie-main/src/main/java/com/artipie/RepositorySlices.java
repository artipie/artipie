/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Key;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Tokens;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import com.artipie.settings.Settings;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.settings.repo.RepositoriesFromStorage;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class RepositorySlices {

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Tokens: authentication and generation.
     */
    private final Tokens tokens;

    /**
     *
     */
    private final LoadingCache<SliceKey, SliceValue> slices;

    /**
     * New RepositorySlices.
     *
     * @param settings Artipie settings
     * @param tokens Tokens: authentication and generation
     */
    public RepositorySlices(
        final Settings settings,
        final Tokens tokens
    ) {
        this.settings = settings;
        this.tokens = tokens;
        this.slices = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .removalListener(
                (RemovalListener<SliceKey, SliceValue>) notification -> notification.getValue()
                    .client()
                    .ifPresent(JettyClientSlices::stop)
            )
            .build(
                new CacheLoader<>() {
                    @Override
                    public SliceValue load(final SliceKey key) {
                        return resolve(key.name(), key.port());
                    }
                }
            );
    }

    public Slice slice(final Key name, final int port) {
        return this.slices.getUnchecked(new SliceKey(name, port)).slice();
    }

    record SliceKey(Key name, int port) {
    }

    record SliceValue(Slice slice, Optional<JettyClientSlices> client) {

    }

    /**
     * Resolve {@link Slice} by provided configuration.
     *
     * @param name Repository name
     * @param port Repository port
     * @return Slice for repo
     */
    private SliceValue resolve(final Key name, final int port) {
        final RepoConfig cfg = new RepositoriesFromStorage(this.settings)
            .config(name.string()).toCompletableFuture().join();
        if (cfg.port().isEmpty() || cfg.port().getAsInt() == port) {
            final JettyClientSlices client = new JettyClientSlices(
                cfg.httpClientSettings().orElseGet(settings::httpClientSettings)
            );
            client.start();
            return new SliceValue(new SliceFromConfig(
                client,
                this.settings,
                cfg, cfg.port().isPresent(),
                this.tokens),
                Optional.of(client)
            );
        }
        return new SliceValue(new SliceSimple(new RsRepoNotFound(name)), Optional.empty());
    }

    /**
     * Repo not found response.
     *
     * @since 0.9
     */
    private static final class RsRepoNotFound extends Response.Wrap {

        /**
         * New repo not found response.
         *
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
