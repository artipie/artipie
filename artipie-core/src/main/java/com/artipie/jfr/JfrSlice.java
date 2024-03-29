/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.util.concurrent.CompletableFuture;

/**
 * Slice wrapper to generate JFR events for every the {@code response} method call.
 */
public final class JfrSlice implements Slice {

    private final Slice original;

    /**
     * @param original Original slice.
     */
    public JfrSlice(final Slice original) {
        this.original = original;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(
        RequestLine line, Headers headers, Content body
    ) {
        final SliceResponseEvent event = new SliceResponseEvent();
        if (event.isEnabled()) {
            return this.wrapResponse(line, headers, body, event);
        }
        return this.original.response(line, headers, body);
    }

    /**
     * Executes request and fills an event data.
     *
     * @param line The request line
     * @param headers The request headers
     * @param body The request body
     * @param event JFR event
     * @return The response.
     */
    private CompletableFuture<ResponseImpl> wrapResponse(
        RequestLine line,
        Headers headers,
        Content body,
        SliceResponseEvent event
    ) {
        event.begin();
        return this.original.response(
            line, headers,
            new Content.From(
                new ChunksAndSizeMetricsPublisher(
                    body,
                    (chunks, size) -> {
                        event.requestChunks = chunks;
                        event.requestSize = size;
                    }
                )
            )
        ).thenApply(response -> ResponseBuilder.from(response.status())
            .headers(response.headers())
            .body(new Content.From(
                new ChunksAndSizeMetricsPublisher(response.body(), (chunks, size) -> {
                    event.end();
                    if (event.shouldCommit()) {
                        event.method = line.method().value();
                        event.path = line.uri().getPath();
                        event.headers = headers.asString();
                        event.responseChunks = chunks;
                        event.responseSize = size;
                        event.commit();
                    }
                }))).build()
        );
    }

}
