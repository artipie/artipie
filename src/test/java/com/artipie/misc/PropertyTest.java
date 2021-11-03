/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
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
 */
final class PropertyTest {
    @Test
    void readsDefaultValue() {
        final long defval = 500;
        MatcherAssert.assertThat(
            new Property("not.existed.value.so.use.default")
                .asLongOrDefault(String.valueOf(defval)),
            new IsEqual<>(defval)
        );
    }

    @Test
    void readsValueFromArtipieProperties() {
        MatcherAssert.assertThat(
            new Property(ArtipieProperties.STORAGE_TIMEOUT)
                .asLongOrDefault("123"),
            //@checkstyle MagicNumberCheck (1 line)
            new IsEqual<>(180_000L)
        );
    }

    @Test
    void readsValueFromSetProperties() {
        final long val = 17L;
        System.setProperty(ArtipieProperties.AUTH_TIMEOUT, String.valueOf(val));
        MatcherAssert.assertThat(
            new Property(ArtipieProperties.AUTH_TIMEOUT)
                .asLongOrDefault("345"),
            new IsEqual<>(val)
        );
    }

    @Test
    void failsToParseWrongValueFromSetProperties() {
        final String key = "my.property.value";
        System.setProperty(key, "can't be parsed");
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new Property(key).asLongOrDefault("50")
        );
    }

    @Test
    void failsToParseWrongValueFromArtipieProperties() {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new Property(ArtipieProperties.VERSION_KEY)
                .asLongOrDefault("567")
        );
    }
}
