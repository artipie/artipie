/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.lock.Lock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Operation performed under lock.
 *
 * @param <T> Operation result type.
 * @since 0.27
 */
public final class UnderLockOperation<T> {

    /**
     * Lock.
     */
    private final Lock lock;

    /**
     * Operation.
     */
    private final Function<Storage, CompletionStage<T>> operation;

    /**
     * Ctor.
     *
     * @param lock Lock.
     * @param operation Operation.
     */
    public UnderLockOperation(
        final Lock lock,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        this.lock = lock;
        this.operation = operation;
    }

    /**
     * Perform operation under lock on storage.
     *
     * @param storage Storage.
     * @return Operation result.
     * @checkstyle IllegalCatchCheck (10 lines)
     */
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public CompletionStage<T> perform(final Storage storage) {
        return this.lock.acquire().thenCompose(
            nothing -> {
                CompletionStage<T> result;
                try {
                    result = this.operation.apply(storage);
                } catch (final Throwable throwable) {
                    result = new FailedCompletionStage<>(throwable);
                }
                return result.handle(
                    (value, throwable) -> this.lock.release().thenCompose(
                        released -> {
                            final CompletableFuture<T> future = new CompletableFuture<>();
                            if (throwable == null) {
                                future.complete(value);
                            } else {
                                future.completeExceptionally(throwable);
                            }
                            return future;
                        }
                    )
                ).thenCompose(Function.identity());
            }
        );
    }
}
