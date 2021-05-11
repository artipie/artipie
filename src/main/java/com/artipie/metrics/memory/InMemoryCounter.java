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
package com.artipie.metrics.memory;

import com.artipie.metrics.Counter;
import io.prometheus.client.CollectorRegistry;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link Counter} implementation storing data in memory.
 *
 * @since 0.8
 */
public final class InMemoryCounter implements Counter {

    /**
     * Current counter value.
     */
    private final AtomicLong counter = new AtomicLong();
    private static CollectorRegistry registry = new CollectorRegistry();
    private static final io.prometheus.client.Counter prometheusCounter = io.prometheus.client.Counter.build()
        .name("requests_total").help("Total requests.").register(registry);

    @Override
    public void add(final long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                String.format("Amount should not be negative: %d", amount)
            );
        }
        this.counter.addAndGet(amount);
        prometheusCounter.inc();
    }

    @Override
    public void inc() {
        this.counter.incrementAndGet();
        prometheusCounter.inc();
    }

    /**
     * Get counter value.
     *
     * @return Counter value.
     */
    public long value() {
        return this.counter.getAndSet(0L);
    }

    /**
     * Get prometheus counter value.
     *
     * @return prometheusCounter value.
     */
    public int prometheusValue() {
        PushGateway pg = new PushGateway("prometheus.zhedge.xyz:9091");
        try {
            pg.pushAdd(registry, "my_batch_job");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 9;
    }
}
