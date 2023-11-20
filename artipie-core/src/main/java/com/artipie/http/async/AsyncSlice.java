/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.async;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Asynchronous {@link Slice} implementation.
 * <p>
 * This slice encapsulates {@link CompletionStage} of {@link Slice} and returns {@link Response}.
 * </p>
 * @since 0.4
 */
public final class AsyncSlice implements Slice {

    /**
     * Async slice.
     */
    private final CompletionStage<? extends Slice> slice;

    /**
     * Ctor.
     * @param slice Async slice.
     */
    public AsyncSlice(final CompletionStage<? extends Slice> slice) {
        this.slice = slice;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            this.slice.thenApply(target -> target.response(line, headers, body))
        );
    }
}
