/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentFileName;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * This slice responds with value from storage by key from path.
 * <p>
 * It converts URI path to storage {@link Key}
 * and use it to access storage.
 * </p>
 * @see SliceUpload
 */
public final class SliceDownload implements Slice {

    private final Storage storage;

    /**
     * Path to key transformation.
     */
    private final Function<String, Key> transform;

    /**
     * Slice by key from storage.
     *
     * @param storage Storage
     */
    public SliceDownload(final Storage storage) {
        this(storage, KeyFromPath::new);
    }

    /**
     * Slice by key from storage using custom URI path transformation.
     *
     * @param storage Storage
     * @param transform Transformation
     */
    public SliceDownload(final Storage storage,
        final Function<String, Key> transform) {
        this.storage = storage;
        this.transform = transform;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Publisher<ByteBuffer> body) {
        final Key key = this.transform.apply(line.uri().getPath());
        return new AsyncResponse(
            this.storage.exists(key)
                .thenCompose(
                    exist -> {
                        final CompletionStage<Response> result;
                        if (exist) {
                            result = this.storage.value(key)
                                .thenApply(
                                    content -> new RsFull(
                                        RsStatus.OK,
                                        Headers.from(new ContentFileName(line.uri())),
                                        content
                                    )
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
                )
        );
    }
}
