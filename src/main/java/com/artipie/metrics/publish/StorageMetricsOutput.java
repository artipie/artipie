/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.metrics.publish;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.jcabi.log.Logger;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Storage metrics publisher.
 * @since 0.19
 */
public final class StorageMetricsOutput implements MetricsOutput {

    /**
     * Storage for metrics.
     */
    private final Storage storage;

    /**
     * New storage metrics.
     * @param storage Storage
     */
    public StorageMetricsOutput(final Storage storage) {
        this.storage = storage;
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
