/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.misc;

import com.artipie.ArtipieException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Property}.
 * @since 0.23
 * @checkstyle MagicNumberCheck (500 lines)
 */
final class PropertyTest {
    @Test
    void readsDefaultValue() {
        final long defval = 500L;
        MatcherAssert.assertThat(
            new Property("not.existed.value.so.use.default")
                .asLongOrDefault(defval),
            new IsEqual<>(defval)
        );
    }

    @Test
    void readsValueFromArtipieProperties() {
        MatcherAssert.assertThat(
            new Property(ArtipieProperties.STORAGE_TIMEOUT)
                .asLongOrDefault(123L),
            new IsEqual<>(180_000L)
        );
    }

    @Test
    void readsValueFromSetProperties() {
        final long val = 17L;
        System.setProperty(ArtipieProperties.AUTH_TIMEOUT, String.valueOf(val));
        MatcherAssert.assertThat(
            new Property(ArtipieProperties.AUTH_TIMEOUT)
                .asLongOrDefault(345L),
            new IsEqual<>(val)
        );
    }

    @Test
    void failsToParseWrongValueFromSetProperties() {
        final String key = "my.property.value";
        System.setProperty(key, "can't be parsed");
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new Property(key).asLongOrDefault(50L)
        );
    }

    @Test
    void failsToParseWrongValueFromArtipieProperties() {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new Property(ArtipieProperties.VERSION_KEY)
                .asLongOrDefault(567L)
        );
    }
}
