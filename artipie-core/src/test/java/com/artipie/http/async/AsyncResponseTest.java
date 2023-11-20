/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.async;

import com.artipie.http.Response;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AsyncResponse}.
 *
 * @since 0.8
 */
class AsyncResponseTest {

    @Test
    void shouldSend() {
        MatcherAssert.assertThat(
            new AsyncResponse(
                CompletableFuture.completedFuture(new RsWithStatus(RsStatus.OK))
            ),
            new RsHasStatus(RsStatus.OK)
        );
    }

    @Test
    void shouldPropagateFailure() {
        final CompletableFuture<Response> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException());
        MatcherAssert.assertThat(
            new AsyncResponse(future)
                .send((status, headers, body) -> CompletableFuture.allOf())
                .toCompletableFuture()
                .isCompletedExceptionally(),
            new IsEqual<>(true)
        );
    }
}
