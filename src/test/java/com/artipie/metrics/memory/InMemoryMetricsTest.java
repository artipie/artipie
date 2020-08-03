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

import com.artipie.metrics.publish.MetricsOutput;
import java.util.HashMap;
import java.util.Map;
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
        final TestOutput out = new TestOutput();
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
        final TestOutput out = new TestOutput();
        metrics.publish(out);
        MatcherAssert.assertThat(
            out.counters().keySet(),
            Matchers.containsInAnyOrder(one, two)
        );
    }

    /**
     * Test metrics output.
     * @since 0.19
     */
    private static final class TestOutput implements MetricsOutput {

        /**
         * Counters.
         */
        private final Map<String, Long> cnt;

        /**
         * Gauges.
         */
        private final Map<String, Long> ggs;

        /**
         * Ctor.
         */
        TestOutput() {
            this.cnt = new HashMap<>();
            this.ggs = new HashMap<>();
        }

        @Override
        public void counters(final Map<String, Long> data) {
            for (final Map.Entry<String, Long> entry : data.entrySet()) {
                final String key = entry.getKey();
                this.cnt.put(key, this.cnt.getOrDefault(key, 0L) + entry.getValue());
            }
        }

        @Override
        public void gauges(final Map<String, Long> data) {
            this.ggs.putAll(data);
        }

        /**
         * Counters.
         * @return Map copy
         */
        Map<String, Long> counters() {
            return new HashMap<>(this.cnt);
        }

        /**
         * Gauges.
         * @return Map copy
         */
        Map<String, Long> gauges() {
            return new HashMap<>(this.ggs);
        }
    }
}
