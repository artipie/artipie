/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.rx.RxStorageWrapper;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Observable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Storage synchronization.
 * @since 0.19
 */
public class Copy {

    /**
     * The storage to copy from.
     */
    private final Storage from;

    /**
     * Predicate condition to copy keys.
     */
    private final Predicate<? super Key> predicate;

    /**
     * Ctor.
     *
     * @param from The storage to copy to.
     */
    public Copy(final Storage from) {
        this(from, item -> true);
    }

    /**
     * Ctor.
     * @param from The storage to copy to
     * @param keys The keys to copy
     */
    public Copy(final Storage from, final Collection<Key> keys) {
        this(from, new HashSet<>(keys));
    }

    /**
     * Ctor.
     * @param from The storage to copy to
     * @param keys The keys to copy
     */
    public Copy(final Storage from, final Set<Key> keys) {
        this(from, keys::contains);
    }

    /**
     * Ctor.
     *
     * @param from The storage to copy to
     * @param predicate Predicate to copy items
     */
    public Copy(final Storage from, final Predicate<? super Key> predicate) {
        this.from = from;
        this.predicate = predicate;
    }

    /**
     * Copy keys to the specified storage.
     * @param dest Destination storage
     * @return When copy operation completes
     */
    public CompletableFuture<Void> copy(final Storage dest) {
        final RxStorageWrapper rxdst = new RxStorageWrapper(dest);
        final RxStorageWrapper rxsrc = new RxStorageWrapper(this.from);
        return rxsrc.list(Key.ROOT)
            .map(lst -> lst.stream().filter(this.predicate).collect(Collectors.toList()))
            .flatMapObservable(Observable::fromIterable)
            .flatMapCompletable(
                key -> rxsrc.value(key).flatMapCompletable(content -> rxdst.save(key, content))
            )
            .to(CompletableInterop.await())
            .thenApply(ignore -> (Void) null)
            .toCompletableFuture();
    }
}
