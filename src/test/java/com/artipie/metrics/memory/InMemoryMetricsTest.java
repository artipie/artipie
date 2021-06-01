/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.memory;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InMemoryMetrics}.
 *
 * @since 0.9
 */
class InMemoryMetricsTest {

    @Test
    void shouldCreateCounter() {
        MatcherAssert.assertThat(
            new InMemoryMetrics().counter("my-counter"),
            new IsInstanceOf(InMemoryCounter.class)
        );
    }

    @Test
    void shouldCreateDifferentCounterByName() {
        MatcherAssert.assertThat(
            new InMemoryMetrics().counter("one"),
            new IsNot<>(new IsEqual<>(new InMemoryMetrics().counter("another")))
        );
    }

    @Test
    void shouldReturnSameCounterBySameName() {
        final InMemoryMetrics metrics = new InMemoryMetrics();
        final String name = "a.b.c";
        final InMemoryCounter counter = metrics.counter(name);
        MatcherAssert.assertThat(
            metrics.counter(name),
            new IsEqual<>(counter)
        );
    }

    @Test
    void shouldHaveNoCountersOnCreation() {
        final TestMetricsOutput out = new TestMetricsOutput();
        new InMemoryMetrics().publish(out);
        MatcherAssert.assertThat(
            out.counters().entrySet(),
            new IsEmptyCollection<>()
        );
    }

    @Test
    void shouldReturnAllCounters() {
        final InMemoryMetrics metrics = new InMemoryMetrics();
        final String one = "1";
        final String two = "2";
        metrics.counter(one);
        metrics.counter(two);
        final TestMetricsOutput out = new TestMetricsOutput();
        metrics.publish(out);
        MatcherAssert.assertThat(
            out.counters().keySet(),
            Matchers.containsInAnyOrder(one, two)
        );
    }
}
