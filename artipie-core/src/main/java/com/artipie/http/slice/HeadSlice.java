/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
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
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.StandardRs;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * A {@link Slice} which only serves metadata on Binary files.
 *
 * @since 0.26.2
 * @todo #397:30min Use this class in artipie/files-adapter.
 *  We should replace {@link HeadSlice} of artipie/files-adapter by
 *  this one. Before doing this task, be sure that at least version
 *  1.8.1 of artipie/http has been released.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
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
    private final BiFunction<String, Headers, CompletionStage<Headers>> resheaders;

    /**
     * Ctor.
     *
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
                final URI uri = new RequestLineFrom(line).uri();
                final Key key = transform.apply(uri.getPath());
                return storage.metadata(key)
                    .thenApply(
                        meta -> meta.read(Meta.OP_SIZE)
                            .orElseThrow(() -> new IllegalStateException())
                    ).thenApply(
                        size -> new Headers.From(
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
        final BiFunction<String, Headers, CompletionStage<Headers>> resheaders
    ) {
        this.storage = storage;
        this.transform = transform;
        this.resheaders = resheaders;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            CompletableFuture
                .supplyAsync(new RequestLineFrom(line)::uri)
                .thenCompose(
                    uri -> {
                        final Key key = this.transform.apply(uri.getPath());
                        return this.storage.exists(key)
                            .thenCompose(
                                exist -> {
                                    final CompletionStage<Response> result;
                                    if (exist) {
                                        result = this.resheaders
                                            .apply(line, new Headers.From(headers))
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
