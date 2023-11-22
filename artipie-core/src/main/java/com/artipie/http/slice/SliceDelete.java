/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.http.slice;

import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.StandardRs;
import com.artipie.scheduling.RepositoryEvents;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Delete decorator for Slice.
 *
 * @since 0.16
 */
public final class SliceDelete implements Slice {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Repository events.
     */
    private final Optional<RepositoryEvents> events;

    /**
     * Constructor.
     * @param storage Storage.
     */
    public SliceDelete(final Storage storage) {
        this(storage, Optional.empty());
    }

    /**
     * Constructor.
     * @param storage Storage.
     * @param events Repository events
     */
    public SliceDelete(final Storage storage, final RepositoryEvents events) {
        this(storage, Optional.of(events));
    }

    /**
     * Constructor.
     * @param storage Storage.
     * @param events Repository events
     */
    public SliceDelete(final Storage storage, final Optional<RepositoryEvents> events) {
        this.storage = storage;
        this.events = events;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final KeyFromPath key = new KeyFromPath(new RequestLineFrom(line).uri().getPath());
        return new AsyncResponse(
            this.storage.exists(key).thenCompose(
                exists -> {
                    final CompletableFuture<Response> rsp;
                    if (exists) {
                        rsp = this.storage.delete(key).thenAccept(
                            nothing -> this.events.ifPresent(item -> item.addDeleteEventByKey(key))
                        ).thenApply(none -> StandardRs.NO_CONTENT);
                    } else {
                        rsp = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                    }
                    return rsp;
                }
            )
        );
    }
}
