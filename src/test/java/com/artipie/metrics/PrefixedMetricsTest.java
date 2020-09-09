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
package com.artipie.metrics;

import com.artipie.metrics.memory.InMemoryCounter;
import com.artipie.metrics.memory.InMemoryGauge;
import com.artipie.metrics.publish.MetricsOutput;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PrefixedMetrics}.
 *
 * @since 0.9
 * @checkstyle MethodBodyCommentsCheck (500 lines)
 */
public class PrefixedMetricsTest {

    @Test
    void shouldCreatePrefixedCounter() {
        final AtomicReference<String> captured = new AtomicReference<>();
        new PrefixedMetrics(
            new Metrics() {
                @Override
                public Counter counter(final String name) {
                    captured.set(name);
                    return new InMemoryCounter();
                }

                @Override
                public Gauge gauge(final String name) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void publish(final MetricsOutput out) {
                    // do nothing
                }
            },
            "prefix."
        ).counter("name");
        MatcherAssert.assertThat(
            captured.get(),
            new IsEqual<>("prefix.name")
        );
    }

    @Test
    void shouldCreatePrefixedGauge() {
        final AtomicReference<String> captured = new AtomicReference<>();
        new PrefixedMetrics(
            new Metrics() {
                @Override
                public Counter counter(final String name) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Gauge gauge(final String name) {
                    captured.set(name);
                    return new InMemoryGauge();
                }

                @Override
                public void publish(final MetricsOutput out) {
                    // do nothing
                }
            },
            "gau"
        ).gauge("ge");
        MatcherAssert.assertThat(
            captured.get(),
            new IsEqual<>("gauge")
        );
    }
}
