/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.memory;

import java.util.stream.IntStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InMemoryCounter}.
 *
 * @since 0.8
 */
class InMemoryCounterTest {

    @Test
    void shouldBeInitializedWithZero() {
        MatcherAssert.assertThat(new InMemoryCounter().value(), new IsEqual<>(0L));
    }

    @Test
    void shouldAddValue() {
        final InMemoryCounter counter = new InMemoryCounter();
        final long value = 123L;
        counter.add(value);
        MatcherAssert.assertThat(counter.value(), new IsEqual<>(value));
    }

    @Test
    void shouldFailAddNegativeValue() {
        final InMemoryCounter counter = new InMemoryCounter();
        Assertions.assertThrows(IllegalArgumentException.class, () -> counter.add(-1));
    }

    @Test
    void shouldIncrement() {
        final InMemoryCounter counter = new InMemoryCounter();
        final int count = 5;
        IntStream.rangeClosed(1, count).forEach(ignored -> counter.inc());
        MatcherAssert.assertThat(counter.value(), new IsEqual<>((long) count));
    }
}
