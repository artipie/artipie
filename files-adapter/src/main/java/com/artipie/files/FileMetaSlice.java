/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.files;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqParams;
import com.artipie.http.slice.KeyFromPath;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Slice that returns metadata of a file when user requests it.
 */
public final class FileMetaSlice implements Slice {

    /**
     * Meta parameter.
     */
    private static final String META_PARAM = "meta";

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Slice to wrap.
     */
    private final Slice origin;

    /**
     * Ctor.
     * @param origin Slice to wrap
     * @param storage Storage where to find file
     */
    public FileMetaSlice(final Slice origin, final Storage storage) {
        this.origin = origin;
        this.storage = storage;
    }

    @Override
    public CompletableFuture<Response> response(
        final RequestLine line,
        final Headers iterable,
        final Content publisher
    ) {
        final URI uri = line.uri();
        final Optional<String> meta = new RqParams(uri).value(FileMetaSlice.META_PARAM);
        final CompletableFuture<Response> raw = this.origin.response(line, iterable, publisher);
        final CompletableFuture<Response> result;
        if (meta.isPresent() && Boolean.parseBoolean(meta.get())) {
            final Key key = new KeyFromPath(uri.getPath());
            result = raw.thenCompose(
                resp -> this.storage.exists(key)
                    .thenCompose(exist -> {
                        if (exist) {
                            return this.storage.metadata(key)
                                .thenApply(metadata -> {
                                    ResponseBuilder builder = ResponseBuilder.from(resp.status())
                                        .headers(resp.headers())
                                        .body(resp.body());
                                    from(metadata).stream().forEach(builder::header);
                                    return builder.build();
                                });
                        }
                        return CompletableFuture.completedFuture(resp);
                    }));
        } else {
            result = raw;
        }
        return result;
    }

    /**
     * Headers from meta.
     *
     * @param mtd Meta
     * @return Headers
     */
    private static Headers from(final Meta mtd) {
        final Map<Meta.OpRWSimple<?>, String> fmtd = new HashMap<>();
        fmtd.put(Meta.OP_MD5, "X-Artipie-MD5");
        fmtd.put(Meta.OP_CREATED_AT, "X-Artipie-CreatedAt");
        fmtd.put(Meta.OP_SIZE, "X-Artipie-Size");
        return new Headers(
            fmtd.entrySet().stream()
                .map(entry ->
                    new Header(entry.getValue(), mtd.read(entry.getKey()).orElseThrow().toString()))
                .toList()
        );
    }
}
