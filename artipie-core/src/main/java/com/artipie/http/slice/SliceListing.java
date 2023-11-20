/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * This slice lists blobs contained in given path.
 * <p>
 * It formats response content according to {@link Function}
 * formatter.
 * It also converts URI path to storage {@link Key}
 * and use it to access storage.
 * </p>
 * @since 1.1.1
 * @todo #158:30min Implement HTML standard format.
 *  Currently we have standard enum implementations for simple text and json,
 *  we need implement enum item for HTML format.
 */
public final class SliceListing implements Slice {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Path to key transformation.
     */
    private final Function<String, Key> transform;

    /**
     * Mime type.
     */
    private final String mtype;

    /**
     * Collection of keys to string transformation.
     */
    private final ListingFormat format;

    /**
     * Slice by key from storage.
     *
     * @param storage Storage
     * @param mtype Mime type
     * @param format Format of a key collection
     */
    public SliceListing(
        final Storage storage,
        final String mtype,
        final ListingFormat format
    ) {
        this(storage, KeyFromPath::new, mtype, format);
    }

    /**
     * Slice by key from storage using custom URI path transformation.
     *
     * @param storage Storage
     * @param transform Transformation
     * @param mtype Mime type
     * @param format Format of a key collection
     * @checkstyle ParameterNumberCheck (20 lines)
     */
    public SliceListing(
        final Storage storage,
        final Function<String, Key> transform,
        final String mtype,
        final ListingFormat format
    ) {
        this.storage = storage;
        this.transform = transform;
        this.mtype = mtype;
        this.format = format;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            CompletableFuture
                .supplyAsync(new RequestLineFrom(line)::uri)
                .thenCompose(
                    uri -> {
                        final Key key = this.transform.apply(uri.getPath());
                        return this.storage.list(key)
                            .thenApply(
                                keys -> {
                                    final String text = this.format.apply(keys);
                                    return new RsFull(
                                        RsStatus.OK,
                                        new Headers.From(
                                            new ContentType(
                                                String.format(
                                                    "%s; charset=%s",
                                                    this.mtype,
                                                    StandardCharsets.UTF_8
                                                )
                                            )
                                        ),
                                        new Content.From(
                                            text.getBytes(
                                                StandardCharsets.UTF_8
                                            )
                                        )
                                    );
                                }
                            );
                    }
                )
        );
    }
}
