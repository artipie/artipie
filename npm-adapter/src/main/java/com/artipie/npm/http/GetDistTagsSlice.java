/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.npm.PackageNameFromUrl;

import java.util.concurrent.CompletableFuture;

/**
 * Returns value of the `dist-tags` field from package `meta.json`.
 * Request line to this slice looks like /-/package/@hello%2fsimple-npm-project/dist-tags.
 */
public final class GetDistTagsSlice implements Slice {

    /**
     * Abstract Storage.
     */
    private final Storage storage;

    /**
     * @param storage Abstract storage
     */
    public GetDistTagsSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(final RequestLine line,
                                                    final Headers headers,
                                                    final Content body) {
        final String pkg = new PackageNameFromUrl(
            line.toString().replace("/dist-tags", "").replace("/-/package", "")
        ).value();
        final Key key = new Key.From(pkg, "meta.json");
        return this.storage.exists(key).thenCompose(
            exists -> {
                if (exists) {
                    return this.storage.value(key)
                        .thenCompose(Content::asJsonObjectFuture)
                        .thenApply(json -> ResponseBuilder.ok()
                            .jsonBody(json.getJsonObject("dist-tags"))
                            .build());
                }
                return ResponseBuilder.notFound().completedFuture();
            }
        );
    }
}
