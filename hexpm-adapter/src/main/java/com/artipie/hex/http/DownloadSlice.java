/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLine;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * This slice returns content as bytes by Key from request path.
 */
public final class DownloadSlice implements Slice {
    /**
     * Path to packages.
     */
    static final String PACKAGES = "packages";

    /**
     * Pattern for packages.
     */
    static final Pattern PACKAGES_PTRN =
        Pattern.compile(String.format("/%s/\\S+", DownloadSlice.PACKAGES));

    /**
     * Path to tarballs.
     */
    static final String TARBALLS = "tarballs";

    /**
     * Pattern for tarballs.
     */
    static final Pattern TARBALLS_PTRN =
        Pattern.compile(String.format("/%s/\\S+", DownloadSlice.TARBALLS));

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * @param storage Repository storage.
     */
    public DownloadSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
        final Key.From key = new Key.From(
            line.uri().getPath().replaceFirst("/", "")
        );
        return this.storage.exists(key)
            .thenCompose(exist -> {
                    if (exist) {
                        return this.storage.value(key)
                            .thenApply(
                                value -> ResponseBuilder.ok()
                                    .header(ContentType.mime("application/octet-stream"))
                                    .body(value)
                                    .build()
                            );
                    }
                    return CompletableFuture.completedFuture(ResponseBuilder.notFound().build());
                }
            );
    }
}
