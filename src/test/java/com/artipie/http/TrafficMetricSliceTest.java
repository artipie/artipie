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

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsWithBody;
import com.artipie.metrics.Metrics;
import com.artipie.metrics.memory.InMemoryMetrics;
import com.artipie.metrics.memory.TestMetricsOutput;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link TrafficMetricSlice}.
 *
 * @since 0.10
 */
final class TrafficMetricSliceTest {

    @Test
    void updatesMetrics() throws Exception {
        final Metrics metrics = new InMemoryMetrics();
        final int req = 100;
        final int res = 50;
        new TrafficMetricSlice(
            (line, head, body) -> new RsWithBody(
                Flowable.fromPublisher(body).ignoreElements()
                    .andThen(Flowable.just(ByteBuffer.wrap(new byte[res])))
            ),
            metrics
        ).response(
            new RequestLine(RqMethod.GET, "/foo").toString(),
            Collections.emptyList(),
            Flowable.just(ByteBuffer.wrap(new byte[req]))
        ).send(
            (status, headers, body) ->
                Flowable.fromPublisher(body).ignoreElements().to(CompletableInterop.await())
        ).toCompletableFuture().get();
        final TestMetricsOutput out = new TestMetricsOutput();
        metrics.publish(out);
        MatcherAssert.assertThat(
            "request body size wasn't updated",
            out.counters().get("request.body.size"), Matchers.equalTo((long) req)
        );
        MatcherAssert.assertThat(
            "response body size wasn't updated",
            out.counters().get("response.body.size"), Matchers.equalTo((long) res)
        );
    }
}
