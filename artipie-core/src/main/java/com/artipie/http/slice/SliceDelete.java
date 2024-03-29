/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.scheduling.RepositoryEvents;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Delete decorator for Slice.
 */
public final class SliceDelete implements Slice {

    private final Storage storage;

    private final Optional<RepositoryEvents> events;

    /**
     * @param storage Storage.
     */
    public SliceDelete(final Storage storage) {
        this(storage, Optional.empty());
    }

    /**
     * @param storage Storage.
     * @param events Repository events
     */
    public SliceDelete(final Storage storage, final RepositoryEvents events) {
        this(storage, Optional.of(events));
    }

    /**
     * @param storage Storage.
     * @param events Repository events
     */
    public SliceDelete(final Storage storage, final Optional<RepositoryEvents> events) {
        this.storage = storage;
        this.events = events;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(
        RequestLine line, Headers headers, Content body
    ) {
        final KeyFromPath key = new KeyFromPath(line.uri().getPath());
        return this.storage.exists(key)
            .thenCompose(
                exists -> {
                    final CompletableFuture<ResponseImpl> rsp;
                    if (exists) {
                        rsp = this.storage.delete(key).thenAccept(
                            nothing -> this.events.ifPresent(item -> item.addDeleteEventByKey(key))
                        ).thenApply(none -> ResponseBuilder.noContent().build());
                    } else {
                        rsp = CompletableFuture.completedFuture(ResponseBuilder.notFound().build());
                    }
                    return rsp;
                }
        );
    }
}
