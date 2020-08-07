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
import com.artipie.metrics.publish.MetricsOutput;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link TrafficMetricSlice}.
 *
 * @since 0.10
 */
final class TrafficMetricSliceTest {

    /**
     * Request metrics key.
     */
    private static final String KEY_REQ = "request.body.size";

    /**
     * Response metrics key.
     */
    private static final String KEY_RSP = "response.body.size";

    @Test
    void updatesMetrics() throws Exception {
        final Metrics metrics = new InMemoryMetrics();
        final int size = 100;
        new TrafficMetricSlice((line, head, body) -> new RsWithBody(body), metrics)
            .response(
                new RequestLine(RqMethod.GET, "/foo").toString(),
                Collections.emptyList(),
                Flowable.just(ByteBuffer.wrap(new byte[size]))
            )
            .send(
                (status, headers, body) ->
                    Flowable.fromPublisher(body).ignoreElements().to(CompletableInterop.await())
            ).toCompletableFuture().get();
        final AtomicLong request = new AtomicLong();
        final AtomicLong response = new AtomicLong();
        metrics.publish(
            // @checkstyle AnonInnerLengthCheck (22 lines)
            new MetricsOutput() {
                @Override
                public void counters(final Map<String, Long> data) {
                    if (data.containsKey(TrafficMetricSliceTest.KEY_REQ)) {
                        request.addAndGet(data.get(TrafficMetricSliceTest.KEY_REQ));
                    }
                    if (data.containsKey(TrafficMetricSliceTest.KEY_RSP)) {
                        response.addAndGet(data.get(TrafficMetricSliceTest.KEY_RSP));
                    }
                }

                @Override
                public void gauges(final Map<String, Long> data) {
                    if (data.containsKey(TrafficMetricSliceTest.KEY_REQ)) {
                        request.set(data.get(TrafficMetricSliceTest.KEY_REQ));
                    }
                    if (data.containsKey(TrafficMetricSliceTest.KEY_RSP)) {
                        response.set(data.get(TrafficMetricSliceTest.KEY_RSP));
                    }
                }
            }
        );
        MatcherAssert.assertThat(
            "request body size wasn't updated",
            request.get(), Matchers.equalTo((long) size)
        );
        MatcherAssert.assertThat(
            "response body size wasn't updated",
            response.get(), Matchers.equalTo((long) size)
        );
    }
}
