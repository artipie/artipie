/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.fs;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.UnderLockOperation;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.ext.CompletableFutureSupport;
import com.artipie.asto.lock.storage.StorageLock;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.file.CopyOptions;
import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simple storage, in files.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class VertxFileStorage implements Storage {

    /**
     * Where we keep the data.
     */
    private final Path dir;

    /**
     * The Vert.x.
     */
    private final Vertx vertx;

    /**
     * Storage identifier string (name and path).
     */
    private final String id;

    /**
     * Ctor.
     *
     * @param path The path to the dir
     * @param vertx The Vert.x instance.
     */
    public VertxFileStorage(final Path path, final Vertx vertx) {
        this.dir = path;
        this.vertx = vertx;
        this.id = String.format("Vertx FS: %s", this.dir.toString());
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return Single.fromCallable(
            () -> {
                final Path path = this.path(key);
                return Files.exists(path) && !Files.isDirectory(path);
            }
        ).subscribeOn(RxHelper.blockingScheduler(this.vertx.getDelegate()))
            .to(SingleInterop.get()).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        return Single.fromCallable(
            () -> {
                final Path path = this.path(prefix);
                final Collection<Key> keys;
                if (Files.exists(path)) {
                    final int dirnamelen;
                    if (Key.ROOT.equals(prefix)) {
                        dirnamelen = path.toString().length() + 1;
                    } else {
                        dirnamelen = path.toString().length() - prefix.string().length();
                    }
                    try {
                        keys = Files.walk(path)
                            .filter(Files::isRegularFile)
                            .map(Path::toString)
                            .map(p -> p.substring(dirnamelen))
                            .map(
                                s -> s.split(
                                    FileSystems.getDefault().getSeparator().replace("\\", "\\\\")
                                )
                            )
                            .map(Key.From::new)
                            .sorted(Comparator.comparing(Key.From::string))
                            .collect(Collectors.toList());
                    } catch (final IOException iex) {
                        throw new ArtipieIOException(iex);
                    }
                } else {
                    keys = Collections.emptyList();
                }
                Logger.info(
                    this,
                    "Found %d objects by the prefix \"%s\" in %s by %s: %s",
                    keys.size(), prefix.string(), this.dir, path, keys
                );
                return keys;
            })
            .subscribeOn(RxHelper.blockingScheduler(this.vertx.getDelegate()))
            .to(SingleInterop.get()).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return Single.fromCallable(
            () -> {
                final Path tmp = Paths.get(
                    this.dir.toString(),
                    String.format("%s.%s.tmp", key.string(), UUID.randomUUID())
                );
                tmp.getParent().toFile().mkdirs();
                return tmp;
            })
            .subscribeOn(RxHelper.blockingScheduler(this.vertx.getDelegate()))
            .flatMapCompletable(
                tmp -> new VertxRxFile(
                    tmp,
                    this.vertx
                ).save(Flowable.fromPublisher(content))
                    .andThen(
                        this.vertx.fileSystem()
                            .rxMove(
                                tmp.toString(),
                                this.path(key).toString(),
                                new CopyOptions().setReplaceExisting(true)
                            )
                    )
                    .onErrorResumeNext(
                        throwable -> new VertxRxFile(tmp, this.vertx)
                            .delete()
                            .andThen(Completable.error(throwable))
                    )
            )
            .to(CompletableInterop.await())
            .<Void>thenApply(o -> null)
            .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return Single.fromCallable(
            () -> {
                final Path dest = this.path(destination);
                dest.getParent().toFile().mkdirs();
                return dest;
            })
            .subscribeOn(RxHelper.blockingScheduler(this.vertx.getDelegate()))
            .flatMapCompletable(
                dest -> new VertxRxFile(this.path(source), this.vertx).move(dest)
            )
            .to(CompletableInterop.await())
            .<Void>thenApply(file -> null)
            .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return new VertxRxFile(this.path(key), this.vertx)
            .delete()
            .to(CompletableInterop.await())
            .toCompletableFuture()
            .thenCompose(ignored -> CompletableFuture.allOf());
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final CompletableFuture<Content> res;
        if (Key.ROOT.equals(key)) {
            res = new CompletableFutureSupport.Failed<Content>(
                new ArtipieIOException("Unable to load from root")
            ).get();
        } else {
            res = VertxFileStorage.size(this.path(key)).thenApply(
                size ->
                    new Content.OneTime(
                        new Content.From(
                            size,
                            new VertxRxFile(this.path(key), this.vertx).flow()
                        )
                    )
            );
        }
        return res;
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        return new UnderLockOperation<>(new StorageLock(this, key), operation).perform(this);
    }

    // @checkstyle MissingDeprecatedCheck (5 lines)
    @Deprecated
    @Override
    public CompletableFuture<Long> size(final Key key) {
        return VertxFileStorage.size(this.path(key));
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        return CompletableFuture.completedFuture(Meta.EMPTY);
    }

    @Override
    public String identifier() {
        return this.id;
    }

    /**
     * Resolves key to file system path.
     *
     * @param key Key to be resolved to path.
     * @return Path created from key.
     */
    private Path path(final Key key) {
        return Paths.get(this.dir.toString(), key.string());
    }

    /**
     * File size.
     * @param path File path
     * @return Size
     */
    private static CompletableFuture<Long> size(final Path path) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    return Files.size(path);
                } catch (final NoSuchFileException fex) {
                    throw new ValueNotFoundException(Key.ROOT, fex);
                } catch (final IOException iex) {
                    throw new ArtipieIOException(iex);
                }
            }
        );
    }
}
