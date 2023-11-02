/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.micrometer;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.micrometer.backends.BackendRegistries;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Micrometer storage decorator measures various storage operations execution time.
 * @since 0.28
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class MicrometerStorage implements Storage {

    /**
     * First part name of the metric.
     */
    private static final String ARTIPIE_STORAGE = "artipie.storage";

    /**
     * Origin source storage.
     */
    private final Storage origin;

    /**
     * Micrometer registry.
     */
    private final MeterRegistry registry;

    /**
     * Ctor.
     * @param origin Origin source storage
     * @param registry Micrometer registry
     */
    public MicrometerStorage(final Storage origin, final MeterRegistry registry) {
        this.origin = origin;
        this.registry = registry;
    }

    /**
     * Ctor.
     * @param origin Origin source storage
     */
    public MicrometerStorage(final Storage origin) {
        this(origin, BackendRegistries.getDefaultNow());
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        final Timer.Sample timer = Timer.start(this.registry);
        return this.origin.exists(key).handle(
            (res, err) -> this.handleCompletion("exists", timer, res, err)
        ).thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key key) {
        final Timer.Sample timer = Timer.start(this.registry);
        return this.origin.list(key).handle(
            (res, err) -> this.handleCompletion("list", timer, res, err)
        ).thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        final Timer.Sample timer = Timer.start(this.registry);
        final String method = "save";
        return this.origin.save(
            key, new MicrometerPublisher(content, this.summary(method))
        ).handle(
            (res, err) -> this.handleCompletion(method, timer, res, err)
        ).thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key dest) {
        final Timer.Sample timer = Timer.start(this.registry);
        return this.origin.move(source, dest).handle(
            (res, err) ->
                this.handleCompletion("move", timer, res, err)
        ).thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        final Timer.Sample timer = Timer.start(this.registry);
        return this.origin.metadata(key).handle(
            (res, err) -> this.handleCompletion("metadata", timer, res, err)
        ).thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final Timer.Sample timer = Timer.start(this.registry);
        final String method = "value";
        return this.origin.value(key).handle(
            (res, err) -> this.handleCompletion(method, timer, res, err)
        ).thenCompose(Function.identity())
            .thenApply(content -> new MicrometerPublisher(content, this.summary(method)));
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        final Timer.Sample timer = Timer.start(this.registry);
        return this.origin.delete(key).handle(
            (res, err) -> this.handleCompletion("delete", timer, res, err)
        ).thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<Void> deleteAll(final Key prefix) {
        final Timer.Sample timer = Timer.start(this.registry);
        return this.origin.deleteAll(prefix).handle(
            (res, err) -> this.handleCompletion("deleteAll", timer, res, err)
        ).thenCompose(Function.identity());
    }

    @Override
    public <T> CompletionStage<T> exclusively(final Key key,
        final Function<Storage, CompletionStage<T>> function) {
        final Timer.Sample timer = Timer.start(this.registry);
        return this.origin.exclusively(key, function).handle(
            (res, err) -> this.handleCompletion("exclusively", timer, res, err)
        ).thenCompose(Function.identity());
    }

    @Override
    public String identifier() {
        return this.origin.identifier();
    }

    /**
     * Handles operation completion by stopping the timer and reporting the event. Note, that
     * we also have to complete the operation exactly in the same way as if there were no timers.
     * Storage id and key tags are added by default, provide additional tags in the corresponding
     * parameters.
     * @param method The method name
     * @param timer Timer
     * @param res Operation result
     * @param err Error
     * @param <T> Result type
     * @return Completion stage with the result
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    private <T> CompletionStage<T> handleCompletion(
        final String method, final Timer.Sample timer, final T res, final Throwable err
    ) {
        final CompletionStage<T> complete;
        final String operation = String.join(".", MicrometerStorage.ARTIPIE_STORAGE, method);
        if (err == null) {
            timer.stop(this.registry.timer(operation, "id", this.identifier()));
            complete = CompletableFuture.completedFuture(res);
        } else {
            timer.stop(
                this.registry.timer(
                    String.join(".", operation, "error"), "id", this.identifier()
                )
            );
            complete = CompletableFuture.failedFuture(err);
        }
        return complete;
    }

    /**
     * Create and register distribution summary.
     * @param method Method name
     * @return Summary
     */
    private DistributionSummary summary(final String method) {
        return DistributionSummary
            .builder(String.join(".", MicrometerStorage.ARTIPIE_STORAGE, method, "size"))
            .description("Storage content body size and chunks")
            .tag("id", this.identifier())
            .baseUnit("bytes")
            .register(this.registry);
    }
}
