/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.prometheus;

import io.prometheus.client.CollectorRegistry;
import java.util.stream.IntStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PrometheusCounter}.
 *
 * @since 0.8
 */
class PrometheusCounterTest {

    /**
     * Current name.
     */
    private static String name = "name";

    @Test
    void shouldBeInitializedWithZero() {
        MatcherAssert.assertThat(
            new PrometheusCounter(
                io.prometheus.client.Counter.build().name(PrometheusCounterTest.name)
                    .help(PrometheusCounterTest.name).register(new CollectorRegistry())
            ).value(),
            new IsEqual<>(0L)
        );
    }

    @Test
    void shouldAddValue() {
        final PrometheusCounter counter =
            new PrometheusCounter(
                io.prometheus.client.Counter.build().name(PrometheusCounterTest.name)
                    .help(PrometheusCounterTest.name).register(new CollectorRegistry())
            );
        final long value = 123L;
        counter.add(value);
        MatcherAssert.assertThat(counter.value(), new IsEqual<>(value));
    }

    @Test
    void shouldFailAddNegativeValue() {
        final PrometheusCounter counter =
            new PrometheusCounter(
                io.prometheus.client.Counter.build().name(PrometheusCounterTest.name)
                    .help(PrometheusCounterTest.name).register(new CollectorRegistry())
            );
        Assertions.assertThrows(IllegalArgumentException.class, () -> counter.add(-1));
    }

    @Test
    void shouldIncrement() {
        final PrometheusCounter counter =
            new PrometheusCounter(
                io.prometheus.client.Counter.build().name(PrometheusCounterTest.name)
                    .help(PrometheusCounterTest.name).register(new CollectorRegistry())
            );
        final int count = 5;
        IntStream.rangeClosed(1, count).forEach(ignored -> counter.inc());
        MatcherAssert.assertThat(counter.value(), new IsEqual<>((long) count));
    }
}
