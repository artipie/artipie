/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.cache;

import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import com.artipie.asto.ext.PublisherAs;
import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Remote.WithErrorHandling}.
 * @since 0.32
 */
class RemoteWithErrorHandlingTest {

    @Test
    void returnsContentFromOrigin() {
        final byte[] bytes = "123".getBytes();
        MatcherAssert.assertThat(
            new PublisherAs(
                new Remote.WithErrorHandling(
                    () -> CompletableFuture.completedFuture(
                        Optional.of(new Content.From(bytes))
                    )
                ).get().toCompletableFuture().join().get()
            ).bytes().toCompletableFuture().join(),
            new IsEqual<>(bytes)
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
