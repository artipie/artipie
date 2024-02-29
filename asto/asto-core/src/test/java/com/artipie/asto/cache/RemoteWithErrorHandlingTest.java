/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.cache;

import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Test for {@link Remote.WithErrorHandling}.
 */
class RemoteWithErrorHandlingTest {

    @Test
    void returnsContentFromOrigin() {
        final byte[] bytes = "123".getBytes();
        Assertions.assertArrayEquals(
            bytes,
            new Remote.WithErrorHandling(
                () -> CompletableFuture.completedFuture(
                    Optional.of(new Content.From(bytes))
                )
            ).get().toCompletableFuture().join().orElseThrow().asBytes()
        );
    }

    @Test
    void returnsEmptyOnError() {
        MatcherAssert.assertThat(
            new Remote.WithErrorHandling(
                () -> new FailedCompletionStage<>(new ConnectException("Connection error"))
            ).get().toCompletableFuture().join().isPresent(),
            new IsEqual<>(false)
        );
    }
}
