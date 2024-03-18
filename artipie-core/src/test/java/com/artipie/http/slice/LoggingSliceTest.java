/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import io.reactivex.Flowable;
import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;

/**
 * Tests for {@link LoggingSlice}.
 */
class LoggingSliceTest {

    @Test
    void shouldLogRequestAndResponse() {
        new LoggingSlice(
            Level.INFO,
            new SliceSimple(
                new RsWithHeaders(
                    new RsWithStatus(RsStatus.OK),
                    "Request-Header", "some; value"
                )
            )
        ).response(
            RequestLine.from("GET /v2/ HTTP_1_1"),
            Headers.from(
                new MapEntry<>("Content-Length", "0"),
                new MapEntry<>("Content-Type", "whatever")
            ),
            Flowable.empty()
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
    }

    @Test
    void shouldLogAndPreserveExceptionInSlice() {
        final IllegalStateException error = new IllegalStateException("Error in slice");
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                Throwable.class,
                () -> this.handle(
                    (line, headers, body) -> {
                        throw error;
                    }
                )
            ),
            new IsEqual<>(error)
        );
    }

    @Test
    void shouldLogAndPreserveExceptionInResponse() {
        final IllegalStateException error = new IllegalStateException("Error in response");
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                Throwable.class,
                () -> this.handle(
                    (line, headers, body) -> conn -> {
                        throw error;
                    }
                )
            ),
            new IsEqual<>(error)
        );
    }

    @Test
    void shouldLogAndPreserveAsyncExceptionInResponse() {
        final IllegalStateException error = new IllegalStateException("Error in response async");
        final CompletionStage<Void> result = this.handle(
            (line, headers, body) -> conn -> {
                final CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(error);
                return future;
            }
        );
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                Throwable.class,
                () -> result.toCompletableFuture().join()
            ).getCause(),
            new IsEqual<>(error)
        );
    }

    private CompletionStage<Void> handle(final Slice slice) {
        return new LoggingSlice(Level.INFO, slice).response(
            RequestLine.from("GET /hello/ HTTP/1.1"),
            Headers.EMPTY,
            Flowable.empty()
        ).send((status, headers, body) -> CompletableFuture.allOf());
    }
}
