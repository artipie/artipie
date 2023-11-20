/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.composer.Repository;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice for uploading archive by key from storage.
 * @since 0.4
 */
final class DownloadArchiveSlice implements Slice {
    /**
     * Repository.
     */
    private final Repository repos;

    /**
     * Slice by key from storage.
     * @param repository Repository
     */
    DownloadArchiveSlice(final Repository repository) {
        this.repos = repository;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final String path = new RequestLineFrom(line).uri().getPath();
        return new AsyncResponse(
            this.repos.value(new KeyFromPath(path))
                .thenApply(RsWithBody::new)
                .thenApply(rsp -> new RsWithStatus(rsp, RsStatus.OK))
        );
    }
}
