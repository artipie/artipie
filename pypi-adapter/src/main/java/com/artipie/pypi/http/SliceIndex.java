/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLinePrefix;
import com.artipie.http.slice.KeyFromPath;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.reactivex.Single;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * SliceIndex returns formatted html output with index of repository packages.
 */
final class SliceIndex implements Slice {

    /**
     * Artipie artifacts storage.
     */
    private final Storage storage;

    /**
     * @param storage Storage
     */
    SliceIndex(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content publisher) {
        final Key rqkey = new KeyFromPath(line.uri().toString());
        final String prefix = new RequestLinePrefix(rqkey.string(), headers).get();
        return SingleInterop.fromFuture(this.storage.list(rqkey))
            .flatMapPublisher(Flowable::fromIterable)
            .flatMapSingle(
                key -> Single.fromFuture(
                    this.storage.value(key).thenCompose(
                        value -> new ContentDigest(value, Digests.SHA256).hex()
                    ).thenApply(
                        hex -> String.format(
                            "<a href=\"%s#sha256=%s\">%s</a><br/>",
                            String.format("%s/%s", prefix, key.string()),
                            hex,
                            new KeyLastPart(key).get()
                        )
                    )
                )
            )
            .collect(StringBuilder::new, StringBuilder::append)
            .map(
                resp -> ResponseBuilder.ok()
                    .htmlBody(
                        String.format(
                            "<!DOCTYPE html>\n<html>\n  </body>\n%s\n</body>\n</html>", resp.toString()
                        ), StandardCharsets.UTF_8)
                    .build()
            ).to(SingleInterop.get()).toCompletableFuture();
    }

}
