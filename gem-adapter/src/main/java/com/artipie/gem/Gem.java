/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.misc.UncheckedSupplier;
import com.artipie.gem.GemMeta.MetaInfo;
import com.artipie.gem.ruby.RubyGemDependencies;
import com.artipie.gem.ruby.RubyGemIndex;
import com.artipie.gem.ruby.RubyGemMeta;
import com.artipie.gem.ruby.SharedRuntime;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An SDK, which servers gem packages.
 * <p>
 * Performes gem index update using specified indexer implementation.
 * </p>
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class Gem {

    /**
     * Read only set of metadata item names.
     */
    private static final Set<Key> META_NAMES = Collections.unmodifiableSet(
        Stream.of(
            "latest_specs.4.8", "latest_specs.4.8.gz", "prerelease_specs.4.8",
            "prerelease_specs.4.8.gz", "specs.4.8", "specs.4.8.gz"
        ).map(Key.From::new).collect(Collectors.toSet())
    );

    /**
     * Gem repository storage.
     */
    private final Storage storage;

    /**
     * Shared ruby runtime.
     */
    private final SharedRuntime shared;

    /**
     * New Gem SDK with default indexer.
     * @param storage Repository storage.
     */
    public Gem(final Storage storage) {
        this.storage = storage;
        this.shared = new SharedRuntime();
    }

    /**
     * Batch update Ruby gems for repository.
     *
     * @param gem Ruby gem for indexing
     * @return Completable action
     */
    public CompletionStage<Pair<String, String>> update(final Key gem) {
        return newTempDir().thenCompose(
            tmp -> new Copy(
                this.storage, key -> META_NAMES.contains(key) || key.equals(gem)
            ).copy(new FileStorage(tmp)).thenCompose(
                ignore -> this.shared.apply(RubyGemMeta::new)
                    .thenApply(meta -> meta.info(tmp.resolve(gem.string())))
                    .thenCompose(
                        info -> {
                            final RevisionFormat fmt = new RevisionFormat();
                            final String name = info.toString(fmt);
                            return CompletableFuture.supplyAsync(
                                new UncheckedSupplier<>(
                                    () -> Files.move(
                                        tmp.resolve(gem.string()),
                                        gem.parent().map(key -> tmp.resolve(key.string()))
                                            .orElse(tmp).resolve(name)
                                    )
                                )
                            ).thenCompose(
                                path -> this.shared.apply(RubyGemIndex::new)
                                    .thenAccept(index -> index.update(path))
                                ).thenCompose(
                                    ignored -> new Copy(new FileStorage(tmp)).copy(this.storage)
                                ).thenApply(ignored -> new ImmutablePair<>(fmt.name, fmt.version));
                        }
                    )
            ).handle(removeTempDir(tmp))
        );
    }

    /**
     * Gem info data.
     * @param gem Gem name
     * @return Future
     */
    public CompletionStage<MetaInfo> info(final String gem) {
        return newTempDir().thenCompose(
            tmp -> new Copy(this.storage, new GemKeyPredicate(gem))
                .copy(new FileStorage(tmp))
                .thenApply(ignore -> tmp)
        ).thenCompose(
            tmp -> this.shared.apply(RubyGemMeta::new)
                .thenCompose(
                    info -> new FileStorage(tmp).list(Key.ROOT).thenApply(
                        items -> items.stream().findFirst()
                            .map(first -> Paths.get(tmp.toString(), first.string()))
                            .map(path -> info.info(path))
                            .orElseThrow(() -> new ArtipieIOException("gem not found"))
                    )
                ).handle(removeTempDir(tmp))
        );
    }

    /**
     * Retreive and merge dependencies for gems specified.
     * @param gems Set of gem names
     * @return Dependencies binary data
     */
    public CompletionStage<ByteBuffer> dependencies(final Set<? extends String> gems) {
        return newTempDir().thenCompose(
            tmp -> new Copy(
                this.storage, new GemKeyPredicate(gems)
            ).copy(new FileStorage(tmp)).thenCompose(
                ignore -> this.shared.apply(RubyGemDependencies::new).thenCompose(
                    deps -> new FileStorage(tmp).list(Key.ROOT).thenApply(
                        keys -> keys.stream()
                            .map(key -> tmp.resolve(key.string()))
                            .collect(Collectors.toSet())
                    ).thenApply(paths -> new ImmutablePair<>(deps, paths))
                ).thenApply(
                    tuple -> tuple.getLeft().dependencies(tuple.getRight())
                )
            ).handle(removeTempDir(tmp))
        );
    }

    /**
     * Create new temp dir asynchronously.
     * @return Future
     */
    private static CompletionStage<Path> newTempDir() {
        return CompletableFuture.supplyAsync(
            new UncheckedSupplier<>(
                () -> Files.createTempDirectory(Gem.class.getSimpleName())
            )
        );
    }

    /**
     * Handle async result.
     * @param tmpdir Path directory to remove
     * @param <T> Result type
     * @return Function handler
     */
    private static <T> BiFunction<T, Throwable, T> removeTempDir(
        final Path tmpdir) {
        return (res, err) -> {
            try {
                if (tmpdir != null) {
                    FileUtils.deleteDirectory(new File(tmpdir.toString()));
                }
            } catch (final IOException iox) {
                throw new ArtipieIOException(iox);
            }
            if (err != null) {
                throw new CompletionException(err);
            }
            return res;
        };
    }

    /**
     * Revision Gem meta format.
     * @since 1.0
     */
    private static final class RevisionFormat implements GemMeta.MetaFormat {

        /**
         * Gem name.
         */
        private String name;

        /**
         * Gem value.
         */
        private String version;

        @Override
        public void print(final String nme, final String value) {
            if (nme.equals("name")) {
                this.name = value;
            }
            if (nme.equals("version")) {
                this.version = value;
            }
        }

        @Override
        public void print(final String nme, final MetaInfo value) {
            // do nothing
        }

        @Override
        public void print(final String nme, final String[] values) {
            // do nothing
        }

        @Override
        public String toString() {
            return String.format("%s-%s.gem", this.name, this.version);
        }
    }
}
