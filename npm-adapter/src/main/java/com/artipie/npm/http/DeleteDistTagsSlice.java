/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Slice that removes dist-tag to meta.json.
 * @since 0.8
 */
public final class DeleteDistTagsSlice implements Slice {

    /**
     * Dist-tags json field name.
     */
    private static final String FIELD = "dist-tags";

    /**
     * Abstract storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Abstract storage
     */
    public DeleteDistTagsSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> iterable,
        final Publisher<ByteBuffer> body) {
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
                        final CompletableFuture<Response> res;
                        if (exists) {
                            res = this.storage.value(meta)
                                .thenCompose(content -> new PublisherAs(content).asciiString())
                                .thenApply(
                                    str -> Json.createReader(new StringReader(str)).readObject()
                                ).thenApply(
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
                                ).thenCompose(
                                    bytes -> this.storage.save(meta, new Content.From(bytes))
                                ).thenApply(
                                    nothing -> StandardRs.OK
                                );
                        } else {
                            res = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                        }
                        return res;
                    }
                )
            );
        } else {
            resp = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return resp;
    }
}
