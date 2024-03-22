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
import com.artipie.http.rq.RequestLine;
import com.artipie.http.BaseResponse;
import com.artipie.scheduling.RepositoryEvents;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Slice to upload the resource to storage by key from path.
 * @see SliceDownload
 */
public final class SliceUpload implements Slice {

    private final Storage storage;

    /**
     * Path to key transformation.
     */
    private final Function<String, Key> transform;

    /**
     * Repository events.
     */
    private final Optional<RepositoryEvents> events;

    /**
     * Slice by key from storage.
     * @param storage Storage
     */
    public SliceUpload(final Storage storage) {
        this(storage, KeyFromPath::new);
    }

    /**
     * Slice by key from storage using custom URI path transformation.
     * @param storage Storage
     * @param transform Transformation
     */
    public SliceUpload(final Storage storage,
        final Function<String, Key> transform) {
        this(storage, transform, Optional.empty());
    }

    /**
     * Slice by key from storage using custom URI path transformation.
     * @param storage Storage
     * @param events Repository events
     */
    public SliceUpload(final Storage storage,
        final RepositoryEvents events) {
        this(storage, KeyFromPath::new, Optional.of(events));
    }

    /**
     * Slice by key from storage using custom URI path transformation.
     * @param storage Storage
     * @param transform Transformation
     * @param events Repository events
     */
    public SliceUpload(final Storage storage, final Function<String, Key> transform,
        final Optional<RepositoryEvents> events) {
        this.storage = storage;
        this.transform = transform;
        this.events = events;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        Key key = transform.apply(line.uri().getPath());
        CompletableFuture<Void> res = this.storage.save(key, new ContentWithSize(body, headers));
        if (this.events.isPresent()) {
            res = res.thenCompose(
                nothing -> this.storage.metadata(key)
                    .thenApply(meta -> meta.read(Meta.OP_SIZE).orElseThrow())
                    .thenAccept(
                        size -> this.events.get()
                            .addUploadEventByKey(key, size, headers)
                    )
            );
        }
        return new AsyncResponse(
            res.thenApply(rsp -> BaseResponse.created())
        );
    }
}
