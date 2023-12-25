/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.memory;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.OneTimePublisher;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.UnderLockOperation;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.ext.CompletableFutureSupport;
import com.artipie.asto.lock.storage.StorageLock;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

/**
 * Storage implementation for benchmarks. It consists of two different storage:
 * <b>backend</b> which should be {@link InMemoryStorage} and
 * <b>local</b> storage which represents map collection.
 * <p>
 *     Value is obtained from backend storage in case of absence in local.
 *     And after that this obtained value is stored in local storage.
 * </p>
 * <p>
 *     Backend storage in this implementation should be used only for read
 *     operations (e.g. readonly).
 * </p>
 * <p>
 *     This class has set with deleted keys. If key exists in this collection,
 *     this key is considered deleted. It allows to just emulate delete operation.
 * </p>
 * @since 1.1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle NestedIfDepthCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public final class BenchmarkStorage implements Storage {
    /**
     * Backend storage.
     */
    private final InMemoryStorage backend;

    /**
     * Local storage.
     */
    private final NavigableMap<Key, byte[]> local;

    /**
     * Set which contains deleted keys.
     */
    private final Set<Key> deleted;

    /**
     * Ctor.
     * @param backend Backend storage
     */
    public BenchmarkStorage(final InMemoryStorage backend) {
        this.backend = backend;
        this.local = new ConcurrentSkipListMap<>(Key.CMP_STRING);
        this.deleted = ConcurrentHashMap.newKeySet();
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return CompletableFuture.completedFuture(
            this.anyStorageContains(key) && !this.deleted.contains(key)
        );
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key root) {
        return CompletableFuture.supplyAsync(
            () -> {
                final String prefix = root.string();
                final Collection<Key> keys = new TreeSet<>(Key.CMP_STRING);
                final SortedSet<String> bckndkeys = this.backend.data
                    .navigableKeySet()
                    .tailSet(prefix);
                final SortedSet<Key> lclkeys = this.local
                    .navigableKeySet()
                    .tailSet(new Key.From(prefix));
                for (final String keystr : bckndkeys) {
                    if (keystr.startsWith(prefix)) {
                        if (!this.deleted.contains(new Key.From(keystr))) {
                            keys.add(new Key.From(keystr));
                        }
                    } else {
                        break;
                    }
                }
                for (final Key key : lclkeys) {
                    if (key.string().startsWith(prefix)) {
                        if (!this.deleted.contains(key)) {
                            keys.add(key);
                        }
                    } else {
                        break;
                    }
                }
                return keys;
            }
        );
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        final CompletableFuture<Void> res;
        if (Key.ROOT.equals(key)) {
            res = new CompletableFutureSupport.Failed<Void>(
                new ArtipieIOException("Unable to save to root")
            ).get();
        } else {
            res = new Concatenation(new OneTimePublisher<>(content)).single()
                .to(SingleInterop.get())
                .thenApply(Remaining::new)
                .thenApply(Remaining::bytes)
                .thenAccept(bytes -> this.local.put(key, bytes))
                .thenAccept(noth -> this.deleted.remove(key))
                .toCompletableFuture();
        }
        return res;
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        final CompletionStage<Void> res;
        if (this.deleted.contains(source)) {
            res = ioErrorCompletion("No value for source key", source);
        } else {
            final byte[] lcl = this.local.computeIfAbsent(
                source, key -> this.backend.data.get(key.string())
            );
            if (lcl == null) {
                res = ioErrorCompletion("No value for source key", source);
            } else {
                this.local.put(destination, lcl);
                this.local.remove(source);
                this.deleted.remove(destination);
                res = CompletableFuture.allOf();
            }
        }
        return res.toCompletableFuture();
    }

    // @checkstyle MissingDeprecatedCheck (5 lines)
    @Override
    @Deprecated
    public CompletableFuture<Long> size(final Key key) {
        final CompletionStage<Long> res;
        if (this.deleted.contains(key) || !this.anyStorageContains(key)) {
            res = notFoundCompletion(key);
        } else {
            if (this.local.containsKey(key)) {
                res = CompletableFuture.completedFuture((long) this.local.get(key).length);
            } else {
                res = CompletableFuture.completedFuture(
                    (long) this.backend.data.get(key.string()).length
                );
            }
        }
        return res.toCompletableFuture();
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        final CompletableFuture<Meta> res;
        if (this.deleted.contains(key) || !this.anyStorageContains(key)) {
            res = new FailedCompletionStage<Meta>(new ValueNotFoundException(key))
                .toCompletableFuture();
        } else {
            res = new CompletableFuture<>();
            res.complete(Meta.EMPTY);
        }
        return res.toCompletableFuture();
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final CompletionStage<Content> res;
        if (Key.ROOT.equals(key)) {
            res = new FailedCompletionStage<>(new ArtipieIOException("Unable to load from root"));
        } else {
            if (this.deleted.contains(key)) {
                res = notFoundCompletion(key);
            } else {
                final byte[] lcl = this.local.computeIfAbsent(
                    key, ckey -> this.backend.data.get(ckey.string())
                );
                if (lcl == null) {
                    res = notFoundCompletion(key);
                } else {
                    if (this.deleted.contains(key)) {
                        res = notFoundCompletion(key);
                    } else {
                        res = CompletableFuture.completedFuture(
                            new Content.OneTime(new Content.From(lcl))
                        );
                    }
                }
            }
        }
        return res.toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        final CompletionStage<Void> res;
        if (this.anyStorageContains(key)) {
            final boolean added = this.deleted.add(key);
            if (added) {
                res = CompletableFuture.allOf();
            } else {
                res = ioErrorCompletion("Key does not exist", key);
            }
        } else {
            res = ioErrorCompletion("Key does not exist", key);
        }
        return res.toCompletableFuture();
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        return new UnderLockOperation<>(new StorageLock(this, key), operation).perform(this);
    }

    /**
     * Verify whether key exists in local or backend storage.
     * @param key Key for check
     * @return True if key exists in local or backend storage, false otherwise.
     */
    private boolean anyStorageContains(final Key key) {
        return this.local.containsKey(key) || this.backend.data.containsKey(key.string());
    }

    /**
     * Obtains failed completion for not found key.
     * @param key Not found key
     * @param <T> Ignore
     * @return Failed completion for not found key.
     */
    private static <T> CompletionStage<T> notFoundCompletion(final Key key) {
        return new FailedCompletionStage<>(new ValueNotFoundException(key));
    }

    /**
     * Obtains failed completion for absent key.
     * @param msg Text message for exception
     * @param key Not found key
     * @param <T> Ignore
     * @return Failed completion for absent key.
     */
    private static <T> CompletionStage<T> ioErrorCompletion(final String msg, final Key key) {
        return new FailedCompletionStage<>(
            new ArtipieIOException(String.format("%s: %s", msg, key.string()))
        );
    }
}
