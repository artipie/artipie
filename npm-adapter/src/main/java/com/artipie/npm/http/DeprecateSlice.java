/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.StandardRs;
import com.artipie.npm.PackageNameFromUrl;
import com.artipie.npm.misc.JsonFromPublisher;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;

/**
 * Slice to handle `npm deprecate` command requests.
 * @since 0.8
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
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> iterable,
        final Publisher<ByteBuffer> publisher
    ) {
        final String pkg = new PackageNameFromUrl(line).value();
        final Key key = new Key.From(pkg, "meta.json");
        return new AsyncResponse(
            this.storage.exists(key).thenCompose(
                exists -> {
                    final CompletionStage<Response> res;
                    if (exists) {
                        res = new JsonFromPublisher(publisher).json()
                            .thenApply(json -> json.getJsonObject("versions"))
                            .thenCombine(
                                this.storage.value(key)
                                    .thenApply(JsonFromPublisher::new)
                                    .thenCompose(JsonFromPublisher::json),
                                (body, meta) -> DeprecateSlice.deprecate(body, meta).toString()
                            ).thenCompose(
                                str -> this.storage.save(
                                    key, new Content.From(str.getBytes(StandardCharsets.UTF_8))
                                )
                            )
                            .thenApply(nothing -> StandardRs.OK);
                    } else {
                        res = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                    }
                    return res;
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
