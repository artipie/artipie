/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.misc;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link UncheckedFunc} and {@link UncheckedIOFunc}.
 * @since 1.1
 */
class UncheckedFuncTest {

    @Test
    void throwsArtipieException() {
        final Exception error = new Exception("Error");
        final Exception res = Assertions.assertThrows(
            ArtipieException.class,
            () -> new UncheckedFunc<>(ignored -> { throw error; }).apply("ignored")
        );
        MatcherAssert.assertThat(
            res.getCause(),
            new IsEqual<>(error)
        );
    }

    @Test
    void throwsArtipieIOException() {
        final IOException error = new IOException("IO error");
        final Exception res = Assertions.assertThrows(
            ArtipieIOException.class,
            () -> new UncheckedIOFunc<>(ignored -> { throw error; }).apply("nothing")
        );
        MatcherAssert.assertThat(
            res.getCause(),
            new IsEqual<>(error)
        );
    }

}
