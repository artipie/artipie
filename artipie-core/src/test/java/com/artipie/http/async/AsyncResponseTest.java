/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.async;

import com.artipie.http.Response;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.BaseResponse;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

/**
 * Tests for {@link AsyncResponse}.
 */
class AsyncResponseTest {

    @Test
    void shouldSend() {
        MatcherAssert.assertThat(
            new AsyncResponse(CompletableFuture.completedFuture(BaseResponse.ok())),
            new RsHasStatus(RsStatus.OK)
        );
    }

    @Test
    void shouldPropagateFailure() {
        final CompletableFuture<Response> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException());
        Assertions.assertTrue(
            new AsyncResponse(future)
                .send((status, headers, body) -> CompletableFuture.allOf())
                .toCompletableFuture()
                .isCompletedExceptionally()
        );
    }
}
