/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentFileName;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.ResponseBuilder;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link Slice} which only serves metadata on Binary files.
 */
public final class HeadSlice implements Slice {

    private final Storage storage;

    /**
     * Path to key transformation.
     */
    private final Function<String, Key> transform;

    /**
     * Function to get response headers.
     */
    private final BiFunction<RequestLine, Headers, CompletionStage<Headers>> resHeaders;

    public HeadSlice(final Storage storage) {
        this(storage, KeyFromPath::new);
    }

    /**
     * @param storage Storage
     * @param transform Transformation
     */
    public HeadSlice(final Storage storage, final Function<String, Key> transform) {
        this(
            storage,
            transform,
            (line, headers) -> {
                final URI uri = line.uri();
                final Key key = transform.apply(uri.getPath());
                return storage.metadata(key)
                    .thenApply(
                        meta -> meta.read(Meta.OP_SIZE)
                            .orElseThrow(IllegalStateException::new)
                    ).thenApply(
                        size -> Headers.from(
                            new ContentFileName(uri),
                            new ContentLength(size)
                        )
                    );
            }
        );
    }

    /**
     * @param storage Storage
     * @param transform Transformation
     * @param resHeaders Function to get response headers
     */
    public HeadSlice(
        final Storage storage,
        final Function<String, Key> transform,
        final BiFunction<RequestLine, Headers, CompletionStage<Headers>> resHeaders
    ) {
        this.storage = storage;
        this.transform = transform;
        this.resHeaders = resHeaders;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        final Key key = this.transform.apply(line.uri().getPath());
        CompletableFuture<Response> fut = this.storage.exists(key)
            .thenCompose(
                exist -> {
                    if (exist) {
                        return this.resHeaders
                            .apply(line, headers)
                            .thenApply(res -> ResponseBuilder.ok().headers(res).build());
                    }
                    return CompletableFuture.completedFuture(
                        ResponseBuilder.notFound()
                            .textBody(String.format("Key %s not found", key.string()))
                            .build()
                    );
                }
            );
        return new AsyncResponse(fut);
    }

}
