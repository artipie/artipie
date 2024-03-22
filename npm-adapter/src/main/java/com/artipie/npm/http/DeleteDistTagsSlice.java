/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.BaseResponse;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;

import javax.json.Json;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

/**
 * Slice that removes dist-tag to meta.json.
 */
public final class DeleteDistTagsSlice implements Slice {

    /**
     * Dist-tags json field name.
     */
    private static final String FIELD = "dist-tags";

    private final Storage storage;

    /**
     * @param storage Abstract storage
     */
    public DeleteDistTagsSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(RequestLine line, Headers iterable, Content body) {
        final Matcher matcher = AddDistTagsSlice.PTRN.matcher(line.uri().getPath());
        if (matcher.matches()) {
            final Key meta = new Key.From(matcher.group("pkg"), "meta.json");
            final String tag = matcher.group("tag");
            return new AsyncResponse(
                this.storage.exists(meta).thenCompose(
                    exists -> {
                        if (exists) {
                            return this.storage.value(meta)
                                .thenCompose(Content::asJsonObjectFuture)
                                .thenApply(
                                    json -> Json.createObjectBuilder(json).add(
                                        DeleteDistTagsSlice.FIELD,
                                        Json.createObjectBuilder()
                                            .addAll(
                                                Json.createObjectBuilder(
                                                    json.getJsonObject(DeleteDistTagsSlice.FIELD)
                                                )
                                            ).remove(tag)
                                    ).build()
                                ).thenApply(
                                    json -> json.toString().getBytes(StandardCharsets.UTF_8)
                                ).thenApply(
                                    bytes -> {
                                        this.storage.save(meta, new Content.From(bytes));
                                        return BaseResponse.ok();
                                    }
                                );
                        }
                        return CompletableFuture.completedFuture(
                            BaseResponse.notFound()
                        );
                    }
                )
            );
        }
        return BaseResponse.badRequest();
    }
}
