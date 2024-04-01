/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
                ResponseBuilder.ok().header("Request-Header", "some; value").build()
            )
        ).response(
            RequestLine.from("GET /v2/ HTTP_1_1"),
            Headers.from(
                new MapEntry<>("Content-Length", "0"),
                new MapEntry<>("Content-Type", "whatever")
            ),
            Content.EMPTY
        ).join();
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
                    (line, headers, body) -> {
                        throw error;
                    }
                )
            ),
            new IsEqual<>(error)
        );
    }

    private void handle(Slice slice) {
        new LoggingSlice(Level.INFO, slice)
            .response(RequestLine.from("GET /hello/ HTTP/1.1"), Headers.EMPTY, Content.EMPTY)
            .join();
    }
}
