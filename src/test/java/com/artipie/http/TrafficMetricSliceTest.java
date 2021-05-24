/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
