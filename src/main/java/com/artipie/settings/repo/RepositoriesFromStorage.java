/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.artipie.settings.AliasSettings;
import com.artipie.settings.ConfigFile;
import com.artipie.settings.MetricsContext;
import com.artipie.settings.Settings;
import com.artipie.settings.StorageByAlias;
import com.artipie.settings.cache.StoragesCache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Publisher;

/**
 * Artipie repositories created from {@link Settings}.
 *
 * @since 0.13
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class RepositoriesFromStorage implements Repositories {
    /**
     * Cache for config files.
     */
    private static LoadingCache<FilesContent, Single<String>> configs;

    /**
     * Cache for aliases.
     */
    private static LoadingCache<FilesContent, Single<StorageByAlias>> aliases;

    static {
        final long duration;
        //@checkstyle MagicNumberCheck (1 line)
        duration = new Property(ArtipieProperties.CONFIG_TIMEOUT).asLongOrDefault(120_000L);
        RepositoriesFromStorage.configs = CacheBuilder.newBuilder()
            .expireAfterWrite(duration, TimeUnit.MILLISECONDS)
            .softValues()
            .build(
                new CacheLoader<>() {
                    @Override
                    public Single<String> load(final FilesContent config) {
                        return config.configContent();
                    }
                }
            );
        RepositoriesFromStorage.aliases = CacheBuilder.newBuilder()
            .expireAfterWrite(duration, TimeUnit.MILLISECONDS)
            .softValues()
            .build(
                new CacheLoader<>() {
                    @Override
                    public Single<StorageByAlias> load(final FilesContent alias) {
                        return alias.aliases();
                    }
                }
            );
    }

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Storages cache.
     */
    private final StoragesCache cache;

    /**
     * Ctor.
     *
     * @param settings Artipie settings.
     * @param cache Storages cache.
     */
    public RepositoriesFromStorage(final Settings settings, final StoragesCache cache) {
        this.settings = settings;
        this.cache = cache;
    }

    @Override
    public CompletionStage<RepoConfig> config(final String name) {
        final FilesContent pair = new FilesContent(
            new Key.From(new ConfigFile(name).name()), this.settings.repoConfigsStorage()
        );
        return Single.zip(
            RepositoriesFromStorage.configs.getUnchecked(pair),
            RepositoriesFromStorage.aliases.getUnchecked(pair),
            (data, als) -> SingleInterop.fromFuture(
                this.fromPublisher(
                    als,
                    pair.key,
                    new Content.From(data.getBytes())
                )
            )
        ).flatMap(self -> self).to(SingleInterop.get());
    }

    /**
     * Create async yaml config from content publisher.
     * @param als Storage aliases
     * @param prefix Repository prefix
     * @param pub Yaml content publisher
     * @return Completion stage of yaml
     */
    private CompletionStage<RepoConfig> fromPublisher(
        final StorageByAlias als, final Key prefix, final Publisher<ByteBuffer> pub
    ) {
        return new Concatenation(pub).single()
            .map(buf -> new Remaining(buf).bytes())
            .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
            .doOnSuccess(yaml -> Logger.debug(RepoConfig.class, "parsed yaml config:\n%s", yaml))
            .map(content -> Yaml.createYamlInput(content.toString()).readYamlMapping())
            .to(SingleInterop.get())
            .thenApply(
                yaml -> new RepoConfig(
                    als,
                    prefix,
                    yaml,
                    this.cache,
                    new MetricsContext(this.settings.meta()).enabled()
                )
            );
    }

    /**
     * Extra class for obtaining aliases and content of configuration file.
     * @since 0.22
     */
    private static final class FilesContent {
        /**
         * Key.
         */
        private final Key key;

        /**
         * Storage.
         */
        private final Storage storage;

        /**
         * Ctor.
         * @param key Key
         * @param storage Storage
         */
        private FilesContent(final Key key, final Storage storage) {
            this.key = key;
            this.storage = storage;
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            final boolean res;
            if (obj == this) {
                res = true;
            } else if (obj instanceof FilesContent) {
                final FilesContent data = (FilesContent) obj;
                res = Objects.equals(this.key, data.key)
                    && Objects.equals(data.storage, this.storage);
            } else {
                res = false;
            }
            return res;
        }

        /**
         * Obtains content of configuration file.
         * @return Content of configuration file.
         */
        Single<String> configContent() {
            return Single.fromFuture(
                new ConfigFile(this.key).valueFrom(this.storage)
                    .thenApply(PublisherAs::new)
                    .thenCompose(PublisherAs::asciiString)
                    .toCompletableFuture()
            );
        }

        /**
         * Obtains aliases from storage by key.
         * @return Aliases from storage by key.
         */
        Single<StorageByAlias> aliases() {
            return Single.fromFuture(new AliasSettings().find(this.storage, this.key));
        }
    }
}
