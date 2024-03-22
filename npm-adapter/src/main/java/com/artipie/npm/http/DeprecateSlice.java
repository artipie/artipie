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
import com.artipie.npm.PackageNameFromUrl;
import org.apache.commons.lang3.StringUtils;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Slice to handle `npm deprecate` command requests.
 */
public final class DeprecateSlice implements Slice {
    /**
     * Patter for `referer` header value.
     */
    static final Pattern HEADER = Pattern.compile("deprecate.*");

    /**
     * Abstract storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Abstract storage
     */
    public DeprecateSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(RequestLine line, Headers iterable, Content publisher) {
        final String pkg = new PackageNameFromUrl(line).value();
        final Key key = new Key.From(pkg, "meta.json");
        return new AsyncResponse(
            this.storage.exists(key).thenCompose(
                exists -> {
                    if (exists) {
                        return new Content.From(publisher).asJsonObjectFuture()
                            .thenApply(json -> json.getJsonObject("versions"))
                            .thenCombine(
                                this.storage.value(key)
                                    .thenCompose(Content::asJsonObjectFuture),
                                (body, meta) -> DeprecateSlice.deprecate(body, meta).toString()
                            ).thenApply(
                                str -> {
                                    this.storage.save(
                                        key, new Content.From(str.getBytes(StandardCharsets.UTF_8))
                                    );
                                    return BaseResponse.ok();
                                }
                            );
                    }
                    return CompletableFuture.completedFuture(BaseResponse.notFound());
                }
            )
        );
    }

    /**
     * Adds tag deprecated from request body to meta.json.
     * @param versions Versions json
     * @param meta Meta json from storage
     * @return Meta json with added deprecate tags
     */
    private static JsonObject deprecate(final JsonObject versions, final JsonObject meta) {
        final JsonPatchBuilder res = Json.createPatchBuilder();
        final String field = "deprecated";
        final  String path = "/versions/%s/deprecated";
        for (final String version : versions.keySet()) {
            if (versions.getJsonObject(version).containsKey(field)) {
                if (StringUtils.isEmpty(versions.getJsonObject(version).getString(field))) {
                    res.remove(String.format(path, version));
                } else {
                    res.add(
                        String.format(path, version),
                        versions.getJsonObject(version).getString(field)
                    );
                }
            }
        }
        return res.build().apply(meta);
    }
}
