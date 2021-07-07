/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.prometheus;

import com.artipie.metrics.memory.TestMetricsOutput;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PrometheusMetrics}.
 *
 * @since 0.9
 */
class PrometheusMetricsTest {

    @Test
    void shouldCreateCounter() {
        MatcherAssert.assertThat(
            new PrometheusMetrics().counter("mycounter"),
            new IsInstanceOf(PrometheusCounter.class)
        );
    }

    @Test
    void shouldCreateDifferentCounterByName() {
        MatcherAssert.assertThat(
            new PrometheusMetrics().counter("one"),
            new IsNot<>(new IsEqual<>(new PrometheusMetrics().counter("another")))
        );
    }

    @Test
    void shouldReturnSameCounterBySameName() {
        final PrometheusMetrics metrics = new PrometheusMetrics();
        final String name = "abc";
        final PrometheusCounter counter = metrics.counter(name);
        MatcherAssert.assertThat(
            metrics.counter(name),
            new IsEqual<>(counter)
        );
    }

    @Test
    void shouldPublishAllCounters() {
        final PrometheusMetrics metrics = new PrometheusMetrics();
        final String one = "metric1";
        final String two = "metric2";
        final PrometheusCounter cone = metrics.counter(one);
        cone.inc();
        cone.inc();
        final PrometheusCounter ctwo = metrics.counter(two);
        ctwo.inc();
        ctwo.inc();
        ctwo.inc();
        final TestMetricsOutput out = new TestMetricsOutput();
        metrics.publish(out);
    }
}
