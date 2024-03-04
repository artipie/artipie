/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import org.reactivestreams.Publisher;

import javax.json.Json;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slice that adds dist-tags to meta.json.
 */
final class AddDistTagsSlice implements Slice {

    /**
     * Endpoint request line pattern.
     */
    static final Pattern PTRN =
        Pattern.compile("/-/package/(?<pkg>.*)/dist-tags/(?<tag>.*)");

    /**
     * Dist-tags json field name.
     */
    private static final String DIST_TAGS = "dist-tags";

    /**
     * Abstract storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Abstract storage
     */
    AddDistTagsSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Matcher matcher = AddDistTagsSlice.PTRN.matcher(
            new RequestLineFrom(line).uri().getPath()
        );
        final Response resp;
        if (matcher.matches()) {
            final Key meta = new Key.From(matcher.group("pkg"), "meta.json");
            final String tag = matcher.group("tag");
            resp = new AsyncResponse(
                this.storage.exists(meta).thenCompose(
                    exists -> {
                        if (exists) {
                            return this.storage.value(meta)
                                .thenCompose(Content::asJsonObjectFuture)
                                .thenCombine(
                                    new Content.From(body).asStringFuture(),
                                    (json, val) -> Json.createObjectBuilder(json).add(
                                        AddDistTagsSlice.DIST_TAGS,
                                        Json.createObjectBuilder()
                                            .addAll(
                                                Json.createObjectBuilder(
                                                    json.getJsonObject(AddDistTagsSlice.DIST_TAGS)
                                                )
                                            ).add(tag, val.replaceAll("\"", ""))
                                    ).build()
                                ).thenApply(
                                    json -> {
                                        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                                        this.storage.save(meta, new Content.From(bytes));
                                        return StandardRs.OK;
                                    }
                                );
                        }
                        return CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                    }
                )
            );
        } else {
            resp = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return resp;
    }
}
