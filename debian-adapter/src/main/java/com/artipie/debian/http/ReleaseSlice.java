/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.debian.Config;
import com.artipie.debian.metadata.InRelease;
import com.artipie.debian.metadata.Release;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.util.concurrent.CompletableFuture;

/**
 * Release slice decorator.
 * Checks, whether Release index exists and creates it if necessary.
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
     * @param origin Origin
     * @param asto Storage
     * @param config Repository configuration
     */
    public ReleaseSlice(final Slice origin, final Storage asto, final Config config) {
        this(origin, asto, new Release.Asto(asto, config), new InRelease.Asto(asto, config));
    }

    @Override
    public CompletableFuture<ResponseImpl> response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        return this.storage.exists(this.release.key()).thenCompose(
            exists -> {
                final CompletableFuture<ResponseImpl> res;
                if (exists) {
                    res = this.origin.response(line, headers, body);
                } else {
                    res = this.release.create()
                        .toCompletableFuture()
                        .thenCompose(nothing -> this.inrelease.generate(this.release.key()))
                        .thenCompose(nothing -> this.origin.response(line, headers, body));
                }
                return res;
            }
        );
    }
}
