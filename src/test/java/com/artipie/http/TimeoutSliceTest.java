/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.StandardRs;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Test case for {@link TimeoutSlice}.
 *
 * @since 0.10
 * @checkstyle MagicNumberCheck (500 lines)
 */
final class TimeoutSliceTest {

    /**
     * Infinity loop runnable.
     */
    private static final Runnable LOOP = () -> {
        while (!Thread.currentThread().isInterrupted()) {
            Thread.yield();
        }
    };

    @Test
    @Timeout(3)
    void cancelRequestOnTimeout() {
        MatcherAssert.assertThat(
            new TimeoutSlice(
                (line, headers, body) -> con -> CompletableFuture.runAsync(TimeoutSliceTest.LOOP),
                Duration.ofSeconds(1)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.UNAVAILABLE),
                new RequestLine(RqMethod.GET, "/")
            )
        );
    }

    @Test
    void returnsOriginRequestIfFast() {
        MatcherAssert.assertThat(
            new TimeoutSlice(
                (line, headers, body) -> StandardRs.OK,
                Duration.ofSeconds(1)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, "/")
            )
        );
    }
}
