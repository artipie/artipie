/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.slice.KeyFromPath;

import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Go mod slice: this slice returns json-formatted metadata about go module as
 * described in "JSON-formatted metadata(.info file body) about the latest known version"
 * section of readme.
 */
public final class LatestSlice implements Slice {

    private final Storage storage;

    /**
     * @param storage Storage
     */
    public LatestSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final RequestLine line, final Headers headers,
        final Content body) {
        return new AsyncResponse(
            CompletableFuture.supplyAsync(
                () -> LatestSlice.normalized(line)
            ).thenCompose(
                path -> this.storage.list(new KeyFromPath(path)).thenCompose(this::resp)
            )
        );
    }

    /**
     * Replaces the word latest if it is the last part of the URI path, by v. Then returns the path.
     * @param line Received request line
     * @return A URI path with replaced latest.
     */
    private static String normalized(final RequestLine line) {
        final URI received = line.uri();
        String path = received.getPath();
        final String latest = "latest";
        if (path.endsWith(latest)) {
            path = path.substring(0, path.lastIndexOf(latest)).concat("v");
        }
        return path;
    }

    /**
     * Composes response. It filters .info files from module directory, chooses the greatest
     * version and returns content from the .info file.
     * @param module Module file names list from repository
     * @return Response
     */
    private CompletableFuture<Response> resp(final Collection<Key> module) {
        final Optional<String> info = module.stream().map(Key::string)
            .filter(item -> item.endsWith("info"))
            .max(Comparator.naturalOrder());
        if (info.isPresent()) {
            return this.storage.value(new KeyFromPath(info.get()))
                .thenApply(c -> BaseResponse.ok().header(ContentType.json()).body(c));
        }
        return CompletableFuture.completedFuture(BaseResponse.notFound());
    }
}
