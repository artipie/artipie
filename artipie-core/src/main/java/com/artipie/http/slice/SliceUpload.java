/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.scheduling.RepositoryEvents;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * Slice to upload the resource to storage by key from path.
 * @see SliceDownload
 * @since 0.6
 */
public final class SliceUpload implements Slice {

    /**
     * Storage.
     */
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
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            CompletableFuture.supplyAsync(() -> new RequestLineFrom(line).uri().getPath())
                .thenApply(this.transform)
                .thenCompose(
                    key -> {
                        CompletableFuture<Void> res =
                            this.storage.save(key, new ContentWithSize(body, headers));
                        if (this.events.isPresent()) {
                            res = res.thenCompose(
                                nothing -> this.storage.metadata(key).thenApply(
                                    meta -> meta.read(Meta.OP_SIZE).get()
                                ).thenAccept(
                                    size -> this.events.get()
                                        .addUploadEventByKey(key, size, headers)
                                )
                            );
                        }
                        return res;
                    }
                ).thenApply(rsp -> new RsWithStatus(RsStatus.CREATED))
        );
    }
}
