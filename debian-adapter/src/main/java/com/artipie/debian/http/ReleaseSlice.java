/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.http;

import com.artipie.asto.Storage;
import com.artipie.debian.Config;
import com.artipie.debian.metadata.InRelease;
import com.artipie.debian.metadata.Release;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.artipie.http.rq.RequestLine;
import org.reactivestreams.Publisher;

/**
 * Release slice decorator.
 * Checks, whether Release index exists and creates it if necessary.
 * @since 0.2
 */
public final class ReleaseSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Abstract storage.
     */
    private final Storage storage;

    /**
     * Repository release index.
     */
    private final Release release;

    /**
     * Repository InRelease index.
     */
    private final InRelease inrelease;

    /**
     * Ctor.
     * @param origin Origin
     * @param asto Storage
     * @param release Release index
     * @param inrelease InRelease index
     */
    public ReleaseSlice(final Slice origin, final Storage asto, final Release release,
        final InRelease inrelease) {
        this.origin = origin;
        this.release = release;
        this.storage = asto;
        this.inrelease = inrelease;
    }

    /**
     * Ctor.
     * @param origin Origin
     * @param asto Storage
     * @param config Repository configuration
     */
    public ReleaseSlice(final Slice origin, final Storage asto, final Config config) {
        this(origin, asto, new Release.Asto(asto, config), new InRelease.Asto(asto, config));
    }

    @Override
    public Response response(
        final RequestLine line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            this.storage.exists(this.release.key()).thenCompose(
                exists -> {
                    final CompletionStage<Response> res;
                    if (exists) {
                        res = CompletableFuture.completedFuture(
                            this.origin.response(line, headers, body)
                        );
                    } else {
                        res = this.release.create().thenCompose(
                            nothing ->  this.inrelease.generate(this.release.key())
                        ).thenApply(
                            nothing -> this.origin.response(line, headers, body)
                        );
                    }
                    return res;
                }
            )
        );
    }
}
