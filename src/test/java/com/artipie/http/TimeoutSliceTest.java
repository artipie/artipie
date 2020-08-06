/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
