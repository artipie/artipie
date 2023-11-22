/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.misc;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link NextSafeAvailablePort}.
 * @since 0.9
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.ProhibitPlainJunitAssertionsRule")
final class NextSafeAvailablePortTest {

    @ParameterizedTest
    @ValueSource(ints = {1_023, 49_152})
    void failsByInvalidPort(final int port) {
        final Throwable thrown =
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new NextSafeAvailablePort(port).value()
            );
        MatcherAssert.assertThat(
            thrown.getMessage(),
            new IsEqual<>(
                String.format("Invalid start port: %s", port)
            )
        );
    }

    @Test
    void getNextValue() {
        MatcherAssert.assertThat(
            new NextSafeAvailablePort().value(),
            Matchers.allOf(
                Matchers.greaterThan(1023),
                Matchers.lessThan(49_152)
            )
        );
    }
}
