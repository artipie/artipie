/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.files;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.FromRemoteCache;
import com.artipie.asto.cache.Remote;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.UriClientSlice;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.scheduling.ArtifactEvent;
import io.reactivex.Flowable;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Binary files proxy {@link Slice} implementation.
 */
public final class FileProxySlice implements Slice {

    /**
     * Repository type.
     */
    private static final String REPO_TYPE = "file-proxy";

    /**
     * Remote slice.
     */
    private final Slice remote;

    /**
     * Cache.
     */
    private final Cache cache;

    /**
     * Artifact events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Reository name.
     */
    private final String rname;

    /**
     * New files proxy slice.
     * @param clients HTTP clients
     * @param remote Remote URI
     */
    public FileProxySlice(final ClientSlices clients, final URI remote) {
        this(new UriClientSlice(clients, remote), Cache.NOP, Optional.empty(), FilesSlice.ANY_REPO);
    }

    /**
     * New files proxy slice.
     * @param clients HTTP clients
     * @param remote Remote URI
     * @param auth Authenticator
     * @param asto Cache storage
     */
    public FileProxySlice(final ClientSlices clients, final URI remote,
        final Authenticator auth, final Storage asto) {
        this(
            new AuthClientSlice(new UriClientSlice(clients, remote), auth),
            new FromRemoteCache(asto), Optional.empty(), FilesSlice.ANY_REPO
        );
    }

    /**
     * New files proxy slice.
     * @param clients HTTP clients
     * @param remote Remote URI
     * @param asto Cache storage
     * @param events Artifact events
     * @param rname Repository name
     */
    public FileProxySlice(final ClientSlices clients, final URI remote, final Storage asto,
        final Queue<ArtifactEvent> events, final String rname) {
        this(
            new AuthClientSlice(new UriClientSlice(clients, remote), Authenticator.ANONYMOUS),
            new FromRemoteCache(asto), Optional.of(events), rname
        );
    }

    /**
     * @param remote Remote slice
     * @param cache Cache
     */
    FileProxySlice(final Slice remote, final Cache cache) {
        this(remote, cache, Optional.empty(), FilesSlice.ANY_REPO);
    }

    /**
     * @param remote Remote slice
     * @param cache Cache
     * @param events Artifact events
     * @param rname Repository name
     */
    public FileProxySlice(
        final Slice remote, final Cache cache,
        final Optional<Queue<ArtifactEvent>> events, final String rname
    ) {
        this.remote = remote;
        this.cache = cache;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(
        RequestLine line, Headers ignored, Content pub
    ) {
        final AtomicReference<Headers> headers = new AtomicReference<>();
        final KeyFromPath key = new KeyFromPath(line.uri().getPath());

        return this.cache.load(key,
                new Remote.WithErrorHandling(
                    () -> {
                        final CompletableFuture<Optional<? extends Content>> promise = new CompletableFuture<>();
                        this.remote.response(line, Headers.EMPTY, Content.EMPTY)
                            .thenApply(
                                response -> {
                                    final CompletableFuture<Void> term = new CompletableFuture<>();
                                    headers.set(response.headers());

                                    if (response.status().success()) {
                                        final Flowable<ByteBuffer> body = Flowable.fromPublisher(response.body())
                                            .doOnError(term::completeExceptionally)
                                            .doOnTerminate(() -> term.complete(null));

                                        promise.complete(Optional.of(new Content.From(body)));

                                        if (this.events.isPresent()) {
                                            final long size =
                                                new RqHeaders(headers.get(), ContentLength.NAME)
                                                    .stream().findFirst().map(Long::parseLong)
                                                    .orElse(0L);
                                            this.events.get().add(
                                                new ArtifactEvent(
                                                    FileProxySlice.REPO_TYPE, this.rname, "ANONYMOUS",
                                                    key.string(), "UNKNOWN", size
                                                )
                                            );
                                        }
                                    } else {
                                        promise.complete(Optional.empty());
                                    }
                                    return term;
                                });
                        return promise;
                    }
                ),
                CacheControl.Standard.ALWAYS
            ).toCompletableFuture()
            .handle((content, throwable) -> {
                    if (throwable == null && content.isPresent()) {
                        return ResponseBuilder.ok()
                            .headers(headers.get())
                            .body(content.get())
                            .build();
                    }
                    return ResponseBuilder.notFound().build();
                }
            );
    }
}
