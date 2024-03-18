/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

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
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.StandardRs;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link Slice} which only serves metadata on Binary files.
 */
public final class HeadSlice implements Slice {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Path to key transformation.
     */
    private final Function<String, Key> transform;

    /**
     * Function to get response headers.
     */
    private final BiFunction<RequestLine, Headers, CompletionStage<Headers>> resheaders;

    /**
     * @param storage Storage
     */
    public HeadSlice(final Storage storage) {
        this(
            storage,
            KeyFromPath::new
        );
    }

    /**
     * Ctor.
     *
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
     * Ctor.
     *
     * @param storage Storage
     * @param transform Transformation
     * @param resheaders Function to get response headers
     */
    public HeadSlice(
        final Storage storage,
        final Function<String, Key> transform,
        final BiFunction<RequestLine, Headers, CompletionStage<Headers>> resheaders
    ) {
        this.storage = storage;
        this.transform = transform;
        this.resheaders = resheaders;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            CompletableFuture
                .supplyAsync(line::uri)
                .thenCompose(
                    uri -> {
                        final Key key = this.transform.apply(uri.getPath());
                        return this.storage.exists(key)
                            .thenCompose(
                                exist -> {
                                    final CompletionStage<Response> result;
                                    if (exist) {
                                        result = this.resheaders
                                            .apply(line, headers)
                                                .thenApply(
                                                    hdrs -> new RsWithHeaders(StandardRs.OK, hdrs)
                                                );
                                    } else {
                                        result = CompletableFuture.completedFuture(
                                            new RsWithBody(
                                                StandardRs.NOT_FOUND,
                                                String.format("Key %s not found", key.string()),
                                                StandardCharsets.UTF_8
                                            )
                                        );
                                    }
                                    return result;
                                }
                            );
                    }
                )
        );
    }

}
