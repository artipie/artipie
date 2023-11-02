/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rs.common.RsJson;
import com.artipie.npm.PackageNameFromUrl;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Returns value of the `dist-tags` field from package `meta.json`.
 * Request line to this slice looks like /-/package/@hello%2fsimple-npm-project/dist-tags.
 * @since 0.8
 */
public final class GetDistTagsSlice implements Slice {

    /**
     * Abstract Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage Abstract storage
     */
    public GetDistTagsSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String pkg = new PackageNameFromUrl(
            line.replace("/dist-tags", "").replace("/-/package", "")
        ).value();
        final Key key = new Key.From(pkg, "meta.json");
        return new AsyncResponse(
            this.storage.exists(key).thenCompose(
                exists -> {
                    final CompletableFuture<Response> res;
                    if (exists) {
                        res = this.storage.value(key)
                            .thenCompose(content -> new PublisherAs(content).asciiString())
                            .thenApply(
                                str -> Json.createReader(new StringReader(str)).readObject()
                            )
                            .thenApply(json -> new RsJson(json.getJsonObject("dist-tags")));
                    } else {
                        res = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                    }
                    return res;
                }
            )
        );
    }
}
