/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Routing slice.
 * <p>
 * {@link Slice} implementation which redirect requests to {@link Slice} if {@link RtRule} matched.
 * <p>
 * Usage:
 * <pre><code>
 * new SliceRoute(
 *   new SliceRoute.Path(
 *     new RtRule.ByMethod("GET"), new DownloadSlice(storage)
 *   ),
 *   new SliceRoute.Path(
 *     new RtRule.ByMethod("PUT"), new UploadSlice(storage)
 *   )
 * );
 * </code></pre>
 */
public final class SliceRoute implements Slice {

    /**
     * Routes.
     */
    private final List<RtPath> routes;

    /**
     * New slice route.
     * @param routes Routes
     */
    public SliceRoute(final RtPath... routes) {
        this(Arrays.asList(routes));
    }

    /**
     * New slice route.
     * @param routes Routes
     */
    public SliceRoute(final List<RtPath> routes) {
        this.routes = routes;
    }

    @Override
    public CompletableFuture<Response> response(
        RequestLine line, Headers headers, Content body
    ) {
        return this.routes.stream()
            .map(item -> item.response(line, headers, body))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElse(CompletableFuture.completedFuture(
                ResponseBuilder.notFound().build()
            ));
    }
}
