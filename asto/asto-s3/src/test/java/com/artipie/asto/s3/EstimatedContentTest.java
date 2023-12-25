/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.s3;

import com.artipie.asto.Content;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link EstimatedContentCompliment}.
 *
 * @since 0.1
 */
final class EstimatedContentTest {
    @Test
    void shouldReadUntilLimit() throws ExecutionException, InterruptedException {
        final byte[] data = "xxx".getBytes(StandardCharsets.UTF_8);
        final Content content = new EstimatedContentCompliment(
            new Content.From(
                Optional.empty(),
                new Content.From(data)
            ),
            1
        ).estimate().toCompletableFuture().get();
        MatcherAssert.assertThat(
            content.size(), new IsEqual<>(Optional.empty())
        );
    }

    @Test
    void shouldEvaluateSize() throws ExecutionException, InterruptedException {
        final byte[] data = "yyy".getBytes(StandardCharsets.UTF_8);
        final Content content = new EstimatedContentCompliment(
            new Content.From(
                Optional.empty(),
                new Content.From(data)
            )
        ).estimate().toCompletableFuture().get();
        MatcherAssert.assertThat(
            content.size(), new IsEqual<>(Optional.of((long) data.length))
        );
    }
}
