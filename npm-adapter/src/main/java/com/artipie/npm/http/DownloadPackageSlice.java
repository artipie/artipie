/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.artipie.npm.PackageNameFromUrl;
import com.artipie.npm.Tarballs;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Download package endpoint. Return package metadata, all tarball links will be rewritten
 * based on requested URL.
 */
public final class DownloadPackageSlice implements Slice {

    /**
     * Base URL.
     */
    private final URL base;
    private final Storage storage;

    /**
     * @param base Base URL
     * @param storage Abstract storage
     */
    public DownloadPackageSlice(final URL base, final Storage storage) {
        this.base = base;
        this.storage = storage;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        final String pkg = new PackageNameFromUrl(line).value();
        final Key key = new Key.From(pkg, "meta.json");
        return new AsyncResponse(
            this.storage.exists(key).thenCompose(
                exists -> {
                    if (exists) {
                        return this.storage.value(key)
                            .thenApply(content -> new Tarballs(content, this.base).value())
                            .thenApply(
                                content -> BaseResponse.ok()
                                    .header(new Header("Content-Type", "application/json"))
                                    .body(content)
                            );
                    } else {
                        return CompletableFuture.completedFuture(
                            BaseResponse.notFound()
                        );
                    }
                }
            )
        );
    }
}
