/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
