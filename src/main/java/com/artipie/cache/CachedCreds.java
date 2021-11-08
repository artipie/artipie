/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.cache;

import com.artipie.ArtipieException;
import com.artipie.Settings;
import com.artipie.UsersFromStorageYaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.management.Users;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Implementation of cache for credentials with similar configurations
 * in Artipie settings using {@link LoadingCache}.
 * @since 0.23
 */
final class CachedCreds implements CredsConfigCache {
    /**
     * Cache for storages settings.
     */
    private static LoadingCache<Metadata, Users> creds;

    static {
        CachedCreds.creds = CacheBuilder.newBuilder()
            .expireAfterAccess(
                //@checkstyle MagicNumberCheck (1 line)
                new Property(ArtipieProperties.CREDS_TIMEOUT).asLongOrDefault(180_000L),
                TimeUnit.MILLISECONDS
            ).softValues()
            .build(
                new CacheLoader<>() {
                    @Override
                    public Users load(final Metadata meta) {
                        final Pair<Key, Storage> pair = meta.credsMeta();
                        return new UsersFromStorageYaml(pair.getRight(), pair.getLeft());
                    }
                }
            );
    }

    @Override
    public Users credentials(final Settings settings) {
        return CachedCreds.creds.getUnchecked(new Metadata(settings));
    }

    @Override
    public void invalidateAll() {
        CachedCreds.creds.invalidateAll();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d)",
            this.getClass().getSimpleName(), CachedCreds.creds.size()
        );
    }

    /**
     * Extra class for using metadata information in static section.
     * It is important that storages here are obtained from cache because
     * it allows comparing by keys (e.g. path to file with credentials) and
     * storages. It compares by storages because otherwise it would not be
     * possible to cache different values for the same paths but different storages.
     * @since 0.23
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private static final class Metadata {
        /**
         * Settings of Artipie server.
         */
        private final Settings csettings;

        /**
         * Ctor.
         * @param settings Settings
         */
        Metadata(final Settings settings) {
            this.csettings = settings;
        }

        @Override
        public boolean equals(final Object obj) {
            final boolean res;
            if (this == obj) {
                res = true;
            } else if (obj == null || this.getClass() != obj.getClass()) {
                res = false;
            } else {
                final Pair<Key, Storage> meta = ((Metadata) obj).credsMeta();
                final Pair<Key, Storage> curr = this.credsMeta();
                final boolean keys = Key.CMP_STRING.compare(
                    curr.getLeft(), meta.getLeft()
                ) == 0;
                res = keys && meta.getRight().equals(curr.getRight());
            }
            return res;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.credsMeta().getLeft().string());
        }

        /**
         * Obtains meta information about credentials configuration.
         * @return Information about credentials configuration.
         */
        Pair<Key, Storage> credsMeta() {
            return Pair.of(
                new KeyFromPath(
                    Optional.ofNullable(
                        this.csettings.meta().yamlMapping("credentials")
                    ).flatMap(meta -> Optional.ofNullable(meta.string("path")))
                    .orElseThrow(
                        () -> new ArtipieException(
                            String.format(
                                "Failed to read credentials configuration in \n%s", this.csettings
                            )
                        )
                    )
                ),
                this.csettings.storage()
            );
        }
    }
}
