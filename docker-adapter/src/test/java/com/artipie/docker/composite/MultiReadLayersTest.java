/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.docker.Digest;
import com.artipie.docker.fake.FakeLayers;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link MultiReadLayers}.
 *
 * @since 0.3
 */
final class MultiReadLayersTest {
    @ParameterizedTest
    @CsvSource({
        "empty,empty,false",
        "empty,full,true",
        "full,empty,true",
        "faulty,full,true",
        "full,faulty,true",
        "faulty,empty,false",
        "empty,faulty,false"
    })
    void shouldReturnExpectedValue(final String one, final String two, final boolean present) {
        MatcherAssert.assertThat(
            new MultiReadLayers(
                Arrays.asList(
                    new FakeLayers(one),
                    new FakeLayers(two)
                )
            ).get(new Digest.FromString("123"))
                .toCompletableFuture().join()
                .isPresent(),
            new IsEqual<>(present)
        );
    }
}
