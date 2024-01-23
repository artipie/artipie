/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.lock.storage;

import com.artipie.asto.FailedCompletionStage;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.lock.Lock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * {@link Lock} allowing to obtain lock on target {@link Key} in specified {@link Storage}.
 * Lock is identified by it's unique identifier (UUID), which has to be different for each lock.
 *
 * @since 0.24
 */
public final class StorageLock implements Lock {

    /**
     * Proposals.
     */
    private final Proposals proposals;

    /**
     * Identifier.
     */
    private final String uuid;

    /**
     * Expiration time.
     */
    private final Optional<Instant> expiration;

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param target Target key.
     */
    public StorageLock(final Storage storage, final Key target) {
        this(storage, target, UUID.randomUUID().toString(), Optional.empty());
    }

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param target Target key.
     * @param expiration Expiration time.
     */
    public StorageLock(final Storage storage, final Key target, final Instant expiration) {
        this(storage, target, UUID.randomUUID().toString(), Optional.of(expiration));
    }

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param target Target key.
     * @param uuid Identifier.
     * @param expiration Expiration time.
         */
    public StorageLock(
        final Storage storage,
        final Key target,
        final String uuid,
        final Optional<Instant> expiration
    ) {
        this.proposals = new Proposals(storage, target);
        this.uuid = uuid;
        this.expiration = expiration;
    }

    @Override
    public CompletionStage<Void> acquire() {
        return this.proposals.create(this.uuid, this.expiration).thenCompose(
            nothing -> this.proposals.checkSingle(this.uuid)
        ).handle(
            (nothing, throwable) -> {
                final CompletionStage<Void> result;
                if (throwable == null) {
                    result = CompletableFuture.allOf();
                } else {
                    result = this.release().thenCompose(
                        released -> new FailedCompletionStage<>(throwable)
                    );
                }
                return result;
            }
        ).thenCompose(Function.identity());
    }

    @Override
    public CompletionStage<Void> release() {
        return this.proposals.delete(this.uuid);
    }
}
