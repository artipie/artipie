/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.Content;
import com.artipie.composer.Repository;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.slice.KeyFromPath;

/**
 * Slice for uploading archive by key from storage.
 */
final class DownloadArchiveSlice implements Slice {

    private final Repository repos;

    /**
     * Slice by key from storage.
     * @param repository Repository
     */
    DownloadArchiveSlice(final Repository repository) {
        this.repos = repository;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        return new AsyncResponse(
            this.repos.value(new KeyFromPath(line.uri().getPath()))
                .thenApply(content -> BaseResponse.ok().body(content))
        );
    }
}
