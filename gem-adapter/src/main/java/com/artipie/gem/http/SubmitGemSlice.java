/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.gem.Gem;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.ContentWithSize;
import com.artipie.scheduling.ArtifactEvent;

import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A slice, which servers gem packages.
 */
final class SubmitGemSlice implements Slice {

    /**
     * Repository type.
     */
    private static final String REPO_TYPE = "gem";

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * Gem SDK.
     */
    private final Gem gem;

    /**
     * Artifact events.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Repository name.
     */
    private final String name;

    /**
     * Ctor.
     *
     * @param storage The storage.
     * @param events Artifact events
     * @param name Repository name
     */
    SubmitGemSlice(final Storage storage, final Optional<Queue<ArtifactEvent>> events,
        final String name) {
        this.storage = storage;
        this.gem = new Gem(storage);
        this.events = events;
        this.name = name;
    }

    @Override
    public Response response(final RequestLine line, final Headers headers,
                             final Content body) {
        final Key key = new Key.From(
            "gems", UUID.randomUUID().toString().replace("-", "").concat(".gem")
        );
        return new AsyncResponse(
            this.storage.save(
                key, new ContentWithSize(body, headers)
            ).thenCompose(
                none -> {
                    final CompletionStage<Pair<String, String>> update = this.gem.update(key);
                    if (this.events.isPresent()) {
                        return update.thenCompose(
                            pair -> new RqHeaders(headers, "content-length").stream().findFirst()
                                .map(Long::parseLong).map(CompletableFuture::completedFuture)
                                .orElseGet(
                                    () -> this.storage.metadata(key)
                                        .thenApply(mets -> mets.read(Meta.OP_SIZE).get())
                                ).thenAccept(
                                    size -> this.events.get().add(
                                        new ArtifactEvent(
                                            SubmitGemSlice.REPO_TYPE, this.name,
                                            new Login(headers).getValue(),
                                            pair.getKey(), pair.getValue(), size
                                        )
                                    )
                                )
                        );
                    } else {
                        return update.thenAccept(pair -> { });
                    }
                }
            )
                .thenCompose(none -> this.storage.delete(key))
                .thenApply(none -> new RsWithStatus(RsStatus.CREATED))
        );
    }
}
