/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Completion stage that is failed when created.
 *
 * @param <T> Stage result type.
 * @since 0.30
 */
@Deprecated
public final class FailedCompletionStage<T> implements CompletionStage<T> {

    /**
     * Delegate completion stage.
     */
    private final CompletionStage<T> delegate;

    /**
     * Ctor.
     *
     * @param throwable Failure reason.
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public FailedCompletionStage(final Throwable throwable) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        this.delegate = future;
    }

    @Override
    public <U> CompletionStage<U> thenApply(final Function<? super T, ? extends U> func) {
        return this.delegate.thenApply(func);
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> func) {
        return this.delegate.thenApplyAsync(func);
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(
        final Function<? super T, ? extends U> func,
        final Executor executor
    ) {
        return this.delegate.thenApplyAsync(func, executor);
    }

    @Override
    public CompletionStage<Void> thenAccept(final Consumer<? super T> action) {
        return this.delegate.thenAccept(action);
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action) {
        return this.delegate.thenAcceptAsync(action);
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(
        final Consumer<? super T> action,
        final Executor executor
    ) {
        return this.delegate.thenAcceptAsync(action, executor);
    }

    @Override
    public CompletionStage<Void> thenRun(final Runnable action) {
        return this.delegate.thenRun(action);
    }

    @Override
    public CompletionStage<Void> thenRunAsync(final Runnable action) {
        return this.delegate.thenRunAsync(action);
    }

    @Override
    public CompletionStage<Void> thenRunAsync(final Runnable action, final Executor executor) {
        return this.delegate.thenRunAsync(action, executor);
    }

    @Override
    public <U, V> CompletionStage<V> thenCombine(
        final CompletionStage<? extends U> other,
        final BiFunction<? super T, ? super U, ? extends V> func
    ) {
        return this.delegate.thenCombine(other, func);
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(
        final CompletionStage<? extends U> other,
        final BiFunction<? super T, ? super U, ? extends V> func
    ) {
        return this.delegate.thenCombineAsync(other, func);
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(
        final CompletionStage<? extends U> other,
        final BiFunction<? super T, ? super U, ? extends V> func,
        final Executor executor
    ) {
        return this.delegate.thenCombineAsync(other, func, executor);
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBoth(
        final CompletionStage<? extends U> other,
        final BiConsumer<? super T, ? super U> action
    ) {
        return this.delegate.thenAcceptBoth(other, action);
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(
        final CompletionStage<? extends U> other,
        final BiConsumer<? super T, ? super U> action
    ) {
        return this.delegate.thenAcceptBothAsync(other, action);
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(
        final CompletionStage<? extends U> other,
        final BiConsumer<? super T, ? super U> action,
        final Executor executor
    ) {
        return this.delegate.thenAcceptBothAsync(other, action, executor);
    }

    @Override
    public CompletionStage<Void> runAfterBoth(
        final CompletionStage<?> other,
        final Runnable action
    ) {
        return this.delegate.runAfterBoth(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(
        final CompletionStage<?> other,
        final Runnable action
    ) {
        return this.delegate.runAfterBothAsync(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(
        final CompletionStage<?> other,
        final Runnable action,
        final Executor executor
    ) {
        return this.delegate.runAfterBothAsync(other, action, executor);
    }

    @Override
    public <U> CompletionStage<U> applyToEither(
        final CompletionStage<? extends T> other,
        final Function<? super T, U> func
    ) {
        return this.delegate.applyToEither(other, func);
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(
        final CompletionStage<? extends T> other,
        final Function<? super T, U> func
    ) {
        return this.delegate.applyToEitherAsync(other, func);
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(
        final CompletionStage<? extends T> other,
        final Function<? super T, U> func,
        final Executor executor
    ) {
        return this.delegate.applyToEitherAsync(other, func, executor);
    }

    @Override
    public CompletionStage<Void> acceptEither(
        final CompletionStage<? extends T> other,
        final Consumer<? super T> action
    ) {
        return this.delegate.acceptEither(other, action);
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(
        final CompletionStage<? extends T> other,
        final Consumer<? super T> action
    ) {
        return this.delegate.acceptEitherAsync(other, action);
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(
        final CompletionStage<? extends T> other,
        final Consumer<? super T> action,
        final Executor executor
    ) {
        return this.delegate.acceptEitherAsync(other, action, executor);
    }

    @Override
    public CompletionStage<Void> runAfterEither(
        final CompletionStage<?> other,
        final Runnable action
    ) {
        return this.delegate.runAfterEither(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(
        final CompletionStage<?> other,
        final Runnable action
    ) {
        return this.delegate.runAfterEitherAsync(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(
        final CompletionStage<?> other,
        final Runnable action,
        final Executor executor
    ) {
        return this.delegate.runAfterEitherAsync(other, action, executor);
    }

    @Override
    public <U> CompletionStage<U> thenCompose(
        final Function<? super T, ? extends CompletionStage<U>> func
    ) {
        return this.delegate.thenCompose(func);
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(
        final Function<? super T, ? extends CompletionStage<U>> func
    ) {
        return this.delegate.thenComposeAsync(func);
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(
        final Function<? super T, ? extends CompletionStage<U>> func,
        final Executor executor
    ) {
        return this.delegate.thenComposeAsync(func, executor);
    }

    @Override
    public <U> CompletionStage<U> handle(final BiFunction<? super T, Throwable, ? extends U> func) {
        return this.delegate.handle(func);
    }

    @Override
    public <U> CompletionStage<U> handleAsync(
        final BiFunction<? super T, Throwable, ? extends U> func
    ) {
        return this.delegate.handleAsync(func);
    }

    @Override
    public <U> CompletionStage<U> handleAsync(
        final BiFunction<? super T, Throwable, ? extends U> func,
        final Executor executor
    ) {
        return this.delegate.handleAsync(func, executor);
    }

    @Override
    public CompletionStage<T> whenComplete(final BiConsumer<? super T, ? super Throwable> action) {
        return this.delegate.whenComplete(action);
    }

    @Override
    public CompletionStage<T> whenCompleteAsync(
        final BiConsumer<? super T, ? super Throwable> action
    ) {
        return this.delegate.whenCompleteAsync(action);
    }

    @Override
    public CompletionStage<T> whenCompleteAsync(
        final BiConsumer<? super T, ? super Throwable> action,
        final Executor executor
    ) {
        return this.delegate.whenCompleteAsync(action, executor);
    }

    @Override
    public CompletionStage<T> exceptionally(final Function<Throwable, ? extends T> func) {
        return this.delegate.exceptionally(func);
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return this.delegate.toCompletableFuture();
    }
}
