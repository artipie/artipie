/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.memory;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InMemoryGauge}.
 *
 * @since 0.8
 */
class InMemoryGaugeTest {

    @Test
    void shouldBeInitializedWithZero() {
        MatcherAssert.assertThat(new InMemoryGauge().value(), new IsEqual<>(0L));
    }

    @Test
    void shouldStoreValue() {
        final InMemoryGauge gauge = new InMemoryGauge();
        final long value = 123L;
        gauge.set(value);
        MatcherAssert.assertThat(gauge.value(), new IsEqual<>(value));
    }
}
