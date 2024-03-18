/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
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
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * This slice lists blobs contained in given path.
 * <p>
 * It formats response content according to {@link Function}
 * formatter.
 * It also converts URI path to storage {@link Key}
 * and use it to access storage.
 */
public final class SliceListing implements Slice {

    private final Storage storage;

    /**
     * Path to key transformation.
     */
    private final Function<String, Key> transform;

    /**
     * Mime type.
     */
    private final String mime;

    /**
     * Collection of keys to string transformation.
     */
    private final ListingFormat format;

    /**
     * Slice by key from storage.
     *
     * @param storage Storage
     * @param mime Mime type
     * @param format Format of a key collection
     */
    public SliceListing(
        final Storage storage,
        final String mime,
        final ListingFormat format
    ) {
        this(storage, KeyFromPath::new, mime, format);
    }

    /**
     * Slice by key from storage using custom URI path transformation.
     *
     * @param storage Storage
     * @param transform Transformation
     * @param mime Mime type
     * @param format Format of a key collection
     */
    public SliceListing(
        final Storage storage,
        final Function<String, Key> transform,
        final String mime,
        final ListingFormat format
    ) {
        this.storage = storage;
        this.transform = transform;
        this.mime = mime;
        this.format = format;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Publisher<ByteBuffer> body) {
        final Key key = this.transform.apply(line.uri().getPath());
        return new AsyncResponse(
            this.storage.list(key)
                .thenApply(
                    keys -> {
                        final String text = this.format.apply(keys);
                        return new RsFull(
                            RsStatus.OK,
                            Headers.from(
                                new ContentType(
                                    String.format(
                                        "%s; charset=%s",
                                        this.mime,
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
                )
        );
    }
}
