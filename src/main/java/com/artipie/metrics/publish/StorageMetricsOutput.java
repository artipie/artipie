/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.publish;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.jcabi.log.Logger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Storage metrics publisher.
 * @since 0.19
 */
public final class StorageMetricsOutput implements IntervalMetricOutput {

    /**
     * Storage for metrics.
     */
    private final Storage storage;

    /**
     * Publishing interval.
     */
    private final Duration interval;

    /**
     * New storage metrics.
     * @param storage Storage.
     * @param interval Publishing interval.
     */
    public StorageMetricsOutput(final Storage storage, final Duration interval) {
        this.storage = storage;
        this.interval = interval;
    }

    @Override
    public Duration getInterval() {
        return this.interval;
    }

    @Override
    public void counters(final Map<String, Long> data) {
        for (final Map.Entry<String, Long> metric : data.entrySet()) {
            final Key.From key = new Key.From(metric.getKey());
            this.storage.exists(key).thenCompose(
                exists -> {
                    final CompletionStage<Long> res;
                    if (exists) {
                        res = this.storage.value(key).thenCompose(
                            content -> new PublisherAs(content)
                                .string(StandardCharsets.UTF_8)
                                .thenApply(Long::valueOf)
                        );
                    } else {
                        res = CompletableFuture.completedFuture(0L);
                    }
                    return res;
                }
            ).thenCompose(val -> this.storage.save(key, content(val + metric.getValue())))
                .handle(StorageMetricsOutput::handle);
        }
    }

    @Override
    public void gauges(final Map<String, Long> data) {
        for (final Map.Entry<String, Long> metric : data.entrySet()) {
            this.storage.save(new Key.From(metric.getKey()), content(metric.getValue()))
                .handle(StorageMetricsOutput::handle);
        }
    }

    /**
     * Content for long.
     * @param val Long value
     * @return Content publisher
     */
    private static Content content(final long val) {
        return new Content.From(Long.toString(val).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Handle async result.
     * @param none Void result
     * @param err Error
     * @return Nothing
     */
    private static Void handle(final Void none, final Throwable err) {
        if (err != null) {
            Logger.warn(
                StorageMetricsOutput.class, "Failed to update metric value: %[exception]s", err
            );
        }
        return none;
    }
}
