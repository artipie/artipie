/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.asto.Content;
import com.artipie.asto.Splitting;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import io.reactivex.Flowable;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests to check JfrSlice.
 *
 * @since 0.28
 */
public class JfrSliceTest {

    @Test
    void shouldPublishSliceResponseEvent() {
        final int requestChunks = 5;
        final int requestSize = 5 * 512;
        final int responseChunks = 3;
        final int responseSize = 5 * 256;
        final String path = "/same/path";
        try (RecordingStream rs = new RecordingStream()) {
            final AtomicReference<RecordedEvent> ref = new AtomicReference<>();
            rs.onEvent("artipie.SliceResponse", ref::set);
            rs.startAsync();

            ResponseImpl res = new JfrSlice(
                new TestSlice(
                    ResponseBuilder.ok()
                        .body(JfrSliceTest.content(responseSize, responseChunks))
                        .build()
                )
            ).response(new RequestLine(RqMethod.GET, path), Headers.EMPTY,
                JfrSliceTest.content(requestSize, requestChunks)).join();

            ResponseAssert.checkOk(res);
            Assertions.assertTrue(res.body().asBytes().length > 0);

            Awaitility.waitAtMost(3, TimeUnit.SECONDS)
                .until(() -> ref.get() != null);
            final RecordedEvent evt = ref.get();
            Assertions.assertTrue(evt.getDuration().toNanos() > 0);
            MatcherAssert.assertThat(
                "Incorrect method",
                evt.getString("method"), Is.is(RqMethod.GET.value())
            );
            MatcherAssert.assertThat(
                "Incorrect path",
                evt.getString("path"), Is.is(path)
            );
            MatcherAssert.assertThat(
                "Incorrect request chunks count",
                evt.getInt("requestChunks"), Is.is(requestChunks)
            );
            MatcherAssert.assertThat(
                "Incorrect request size",
                evt.getLong("requestSize"), Is.is((long) requestSize)
            );
            MatcherAssert.assertThat(
                "Incorrect response chunks count",
                evt.getInt("responseChunks"), Is.is(responseChunks)
            );
            MatcherAssert.assertThat(
                "Incorrect response size",
                evt.getLong("responseSize"), Is.is((long) responseSize)
            );
        }
    }

    /**
     * Creates content.
     *
     * @param size Size of content's data.
     * @param chunks Chunks count.
     * @return Content.
     */
    private static Content content(final int size, final int chunks) {
        final byte[] data = new byte[size];
        new Random().nextBytes(data);
        final int rest = size % chunks;
        final int chunkSize = size / chunks + rest;
        return new Content.From(
            Flowable.fromPublisher(new Content.From(data))
                .flatMap(
                    buffer -> new Splitting(
                        buffer,
                        chunkSize
                    ).publisher()
                ));
    }

    /**
     * Simple decorator for Slice.
     */
    private static final class TestSlice implements Slice {

        private final ResponseImpl res;

        TestSlice(final ResponseImpl response) {
            this.res = response;
        }

        @Override
        public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
            Flowable.fromPublisher(body).blockingSubscribe();
            return CompletableFuture.completedFuture(this.res);
        }
    }
}
