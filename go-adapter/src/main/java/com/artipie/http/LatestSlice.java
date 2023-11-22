/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.KeyFromPath;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Go mod slice: this slice returns json-formatted metadata about go module as
 * described in "JSON-formatted metadata(.info file body) about the latest known version"
 * section of readme.
 * @since 0.3
 */
public final class LatestSlice implements Slice {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    public LatestSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
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
    private static String normalized(final String line) {
        final URI received = new RequestLineFrom(line).uri();
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
        final CompletableFuture<Response> res;
        if (info.isPresent()) {
            res = this.storage.value(new KeyFromPath(info.get()))
                .thenApply(RsWithBody::new)
                .thenApply(rsp -> new RsWithHeaders(rsp, "content-type", "application/json"))
                .thenApply(rsp -> new RsWithStatus(rsp, RsStatus.OK));
        } else {
            res = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
        }
        return res;
    }
}
