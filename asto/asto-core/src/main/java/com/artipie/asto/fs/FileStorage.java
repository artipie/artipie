/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.fs;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.OneTimePublisher;
import com.artipie.asto.Storage;
import com.artipie.asto.UnderLockOperation;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.ext.CompletableFutureSupport;
import com.artipie.asto.lock.storage.StorageLock;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.cqfn.rio.file.File;

/**
 * Simple storage, in files.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class FileStorage implements Storage {

    /**
     * Where we keep the data.
     */
    private final Path dir;

    /**
     * Storage string identifier (name and path).
     */
    private final String id;

    /**
     * Ctor.
     * @param path The path to the dir
     * @param nothing Just for compatibility
     * @deprecated Use {@link FileStorage#FileStorage(Path)} ctor instead.
     */
    @Deprecated
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public FileStorage(final Path path, final Object nothing) {
        this(path);
    }

    /**
     * Ctor.
     * @param path The path to the dir
     */
    public FileStorage(final Path path) {
        this.dir = path;
        this.id = String.format("FS: %s", this.dir.toString());
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return this.keyPath(key).thenApplyAsync(
            path -> Files.exists(path) && !Files.isDirectory(path)
        );
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        return this.keyPath(prefix).thenApplyAsync(
            path -> {
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
            }
        );
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return this.keyPath(key).thenApplyAsync(
            path ->  {
                final Path tmp = Paths.get(
                    this.dir.toString(),
                    String.format("%s.%s.tmp", key.string(), UUID.randomUUID())
                );
                tmp.getParent().toFile().mkdirs();
                return ImmutablePair.of(path, tmp);
            }
        ).thenCompose(
            pair -> {
                final Path path = pair.getKey();
                final Path tmp = pair.getValue();
                return new File(tmp).write(
                    new OneTimePublisher<>(content),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ).thenCompose(
                    nothing -> FileStorage.move(tmp, path)
                ).handleAsync(
                    (nothing, throwable) -> {
                        tmp.toFile().delete();
                        if (throwable == null) {
                            return null;
                        } else {
                            throw new ArtipieIOException(throwable);
                        }
                    }
                );
            }
        );
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return this.keyPath(source).thenCompose(
            src -> this.keyPath(destination).thenApply(dst -> ImmutablePair.of(src, dst))
        ).thenCompose(pair -> FileStorage.move(pair.getKey(), pair.getValue()));
    }

    @Override
    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    public CompletableFuture<Void> delete(final Key key) {
        return this.keyPath(key).thenAcceptAsync(
            path -> {
                if (Files.exists(path) && !Files.isDirectory(path)) {
                    try {
                        Files.delete(path);
                        this.deleteEmptyParts(path.getParent());
                    } catch (final IOException iex) {
                        throw new ArtipieIOException(iex);
                    }
                } else {
                    throw new ValueNotFoundException(key);
                }
            }
        );
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        return this.keyPath(key).thenApplyAsync(
            path -> {
                final BasicFileAttributes attrs;
                try {
                    attrs = Files.readAttributes(path, BasicFileAttributes.class);
                } catch (final NoSuchFileException fex) {
                    throw new ValueNotFoundException(key, fex);
                } catch (final IOException iox) {
                    throw new ArtipieIOException(iox);
                }
                return new FileMeta(attrs);
            }
        );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final CompletableFuture<Content> res;
        if (Key.ROOT.string().equals(key.string())) {
            res = new CompletableFutureSupport.Failed<Content>(
                new ArtipieIOException("Unable to load from root")
            ).get();
        } else {
            res = this.metadata(key).thenApply(
                meta -> meta.read(Meta.OP_SIZE).orElseThrow(
                    () -> new ArtipieException(
                        String.format("Size is not available for '%s' key", key.string())
                    )
                )
            ).thenCompose(
                size -> this.keyPath(key).thenApply(path -> ImmutablePair.of(path, size))
            ).thenApply(
                pair -> new Content.OneTime(
                    new Content.From(pair.getValue(), new File(pair.getKey()).content())
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

    @Override
    public String identifier() {
        return this.id;
    }

    /**
     * Removes empty key parts (directories).
     * @param target Directory path
     * @checkstyle NestedIfDepthCheck (20 lines)
     */
    private void deleteEmptyParts(final Path target) {
        final Path dirabs = this.dir.normalize().toAbsolutePath();
        final Path path = target.normalize().toAbsolutePath();
        if (!path.toString().startsWith(dirabs.toString()) || dirabs.equals(path)) {
            return;
        }
        if (Files.isDirectory(path)) {
            boolean again = false;
            try {
                try (Stream<Path> files = Files.list(path)) {
                    if (!files.findFirst().isPresent()) {
                        Files.deleteIfExists(path);
                        again = true;
                    }
                }
                if (again) {
                    this.deleteEmptyParts(path.getParent());
                }
            } catch (final IOException err) {
                throw new ArtipieIOException(err);
            }
        }
    }

    /**
     * Moves file from source path to destination.
     *
     * @param source Source path.
     * @param dest Destination path.
     * @return Completion of moving file.
     */
    private static CompletableFuture<Void> move(final Path source, final Path dest) {
        return CompletableFuture.supplyAsync(
            () -> {
                dest.getParent().toFile().mkdirs();
                return dest;
            }
        ).thenAcceptAsync(
            dst -> {
                try {
                    Files.move(source, dst, StandardCopyOption.REPLACE_EXISTING);
                } catch (final IOException iex) {
                    throw new ArtipieIOException(iex);
                }
            }
        );
    }

    /**
     * Converts key to path.
     * <p>
     * Validates the path is in storage directory and converts it to path.
     * Fails with {@link ArtipieIOException} if key is out of storage location.
     * </p>
     *
     * @param key Key to validate.
     * @return Path future
     */
    private CompletableFuture<? extends Path> keyPath(final Key key) {
        final Path path = this.dir.resolve(key.string());
        final CompletableFuture<Path> res = new CompletableFuture<>();
        if (path.normalize().startsWith(path)) {
            res.complete(path);
        } else {
            res.completeExceptionally(
                new ArtipieIOException(
                    String.format("Entry path is out of storage: %s", key)
                )
            );
        }
        return res;
    }
}
