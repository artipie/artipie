/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.async;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.SliceSimple;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

/**
 * Tests for {@link AsyncSlice}.
 *
 * @since 0.8
 */
class AsyncSliceTest {

    @Test
    void shouldRespond() {
        MatcherAssert.assertThat(
            new AsyncSlice(
                CompletableFuture.completedFuture(
                    new SliceSimple(BaseResponse.ok())
                )
            ).response(new RequestLine("GET", "/"), Headers.EMPTY, Content.EMPTY),
            new RsHasStatus(RsStatus.OK)
        );
    }

    @Test
    void shouldPropagateFailure() {
        final CompletableFuture<Slice> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException());
        Assertions.assertTrue(
            new AsyncSlice(future)
                .response(RequestLine.from("GET /index.html HTTP_1_1"), Headers.EMPTY, Content.EMPTY)
                .send((status, headers, body) -> CompletableFuture.allOf())
                .toCompletableFuture()
                .isCompletedExceptionally()
        );
    }
}
