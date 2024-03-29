/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.group;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.SliceSimple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Test case for {@link GroupSlice}.
 */
final class GroupSliceTest {

    @Test
    @Timeout(1)
    void returnsFirstOrderedSuccessResponse() {
        final String expects = "ok-150";
        ResponseImpl response = new GroupSlice(
            slice(RsStatus.NOT_FOUND, "not-found-250", Duration.ofMillis(250)),
            slice(RsStatus.NOT_FOUND, "not-found-50", Duration.ofMillis(50)),
            slice(RsStatus.OK, expects, Duration.ofMillis(150)),
            slice(RsStatus.NOT_FOUND, "not-found-200", Duration.ofMillis(200)),
            slice(RsStatus.OK, "ok-50", Duration.ofMillis(50)),
            slice(RsStatus.OK, "ok-never", Duration.ofDays(1))
        ).response(new RequestLine(RqMethod.GET, "/"), Headers.EMPTY, Content.EMPTY).join();

        Assertions.assertEquals(RsStatus.OK, response.status());
        Assertions.assertEquals(expects, response.body().asString());
    }

    @Test
    void returnsNotFoundIfAllFails() {
        ResponseImpl res = new GroupSlice(
            slice(RsStatus.NOT_FOUND, "not-found-140", Duration.ofMillis(250)),
            slice(RsStatus.NOT_FOUND, "not-found-10", Duration.ofMillis(50)),
            slice(RsStatus.NOT_FOUND, "not-found-110", Duration.ofMillis(200))
        ).response(new RequestLine(RqMethod.GET, "/foo"), Headers.EMPTY, Content.EMPTY).join();

        Assertions.assertEquals(RsStatus.NOT_FOUND, res.status());
    }

    @Test
    @Timeout(1)
    void returnsNotFoundIfSomeFailsWithException() {
        Slice s = (line, headers, body) -> CompletableFuture.failedFuture(new IllegalStateException());

        Assertions.assertEquals(RsStatus.NOT_FOUND,
            new GroupSlice(s)
                .response(new RequestLine(RqMethod.GET, "/faulty/path"), Headers.EMPTY, Content.EMPTY)
                .join().status());
    }

    private static Slice slice(RsStatus status, String body, Duration delay) {
        return new SliceWithDelay(
            new SliceSimple(ResponseBuilder.from(status).textBody(body).build()), delay
        );
    }

    /**
     * Slice testing decorator to add delay before sending request to origin slice.
     */
    private static final class SliceWithDelay extends Slice.Wrap {

        /**
         * Add delay for slice.
         * @param origin Origin slice
         * @param delay Delay duration
         */
        SliceWithDelay(final Slice origin, final Duration delay) {
            super((line, headers, body) -> CompletableFuture.runAsync(
                () -> {
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (final InterruptedException ignore) {
                        Thread.currentThread().interrupt();
                    }
                }
            ).thenCompose(none -> origin.response(line, headers, body)));
        }
    }
}
