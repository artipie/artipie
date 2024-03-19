/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.async;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;

import java.util.concurrent.CompletionStage;

import com.artipie.http.rq.RequestLine;

/**
 * Asynchronous {@link Slice} implementation.
 * <p>
 * This slice encapsulates {@link CompletionStage} of {@link Slice} and returns {@link Response}.
 */
public final class AsyncSlice implements Slice {

    /**
     * Async slice.
     */
    private final CompletionStage<? extends Slice> slice;

    /**
     * @param slice Async slice.
     */
    public AsyncSlice(final CompletionStage<? extends Slice> slice) {
        this.slice = slice;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        return new AsyncResponse(
            this.slice.thenApply(target -> target.response(line, headers, body))
        );
    }
}
