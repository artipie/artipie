/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.async;

import com.artipie.http.Slice;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.SliceSimple;
import io.reactivex.Flowable;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

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
                    new SliceSimple(new RsWithStatus(RsStatus.OK))
                )
            ).response("", Collections.emptySet(), Flowable.empty()),
            new RsHasStatus(RsStatus.OK)
        );
    }

    @Test
    void shouldPropagateFailure() {
        final CompletableFuture<Slice> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException());
        MatcherAssert.assertThat(
            new AsyncSlice(future)
                .response("GET /index.html HTTP_1_1", Collections.emptySet(), Flowable.empty())
                .send((status, headers, body) -> CompletableFuture.allOf())
                .toCompletableFuture()
                .isCompletedExceptionally(),
            new IsEqual<>(true)
        );
    }
}
