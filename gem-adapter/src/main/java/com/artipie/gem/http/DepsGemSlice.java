/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.gem.Gem;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqParams;
import com.artipie.http.BaseResponse;
import io.reactivex.Flowable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/**
 * Dependency API slice implementation.
 */
final class DepsGemSlice implements Slice {

    /**
     * Repository storage.
     */
    private final Storage repo;

    /**
     * New dependency slice.
     * @param repo Repository storage
     */
    DepsGemSlice(final Storage repo) {
        this.repo = repo;
    }

    @Override
    public Response response(final RequestLine line, final Headers headers,
                             final Content body) {
        return new AsyncResponse(
            new Gem(this.repo).dependencies(
                Collections.unmodifiableSet(
                    new HashSet<>(
                        new RqParams(line.uri().getQuery()).value("gems")
                            .map(str -> Arrays.asList(str.split(",")))
                            .orElse(Collections.emptyList())
                    )
                )
            ).thenApply(
                data -> BaseResponse.ok().body(Flowable.just(data))
            )
        );
    }
}
