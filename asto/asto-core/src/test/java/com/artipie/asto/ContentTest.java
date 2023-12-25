/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import hu.akarnokd.rxjava2.interop.MaybeInterop;
import io.reactivex.Flowable;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link Content}.
 *
 * @since 0.24
 */
final class ContentTest {

    @Test
    void emptyHasNoChunks() {
        MatcherAssert.assertThat(
            Flowable.fromPublisher(Content.EMPTY)
                .singleElement()
                .to(MaybeInterop.get())
                .toCompletableFuture()
                .join(),
            new IsNull<>()
        );
    }

    @Test
    void emptyHasZeroSize() {
        MatcherAssert.assertThat(
            Content.EMPTY.size(),
            new IsEqual<>(Optional.of(0L))
        );
    }
}
