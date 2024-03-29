/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.slice.KeyFromPath;

import javax.json.Json;
import java.util.concurrent.CompletableFuture;

/**
 * Package slice returns info about package, serves on `GET /package/{owner_login}/{package_name}`.
 * @since 0.4
 * @todo #32:30min Implement get package slice to provide package info if the package exists. For
 *  any details check swagger docs:
 *  https://api.anaconda.org/docs#!/package/get_package_owner_login_package_name
 *  Now this slice always returns `package not found` error.
 */
public final class GetPackageSlice implements Slice {

    @Override
    public CompletableFuture<ResponseImpl> response(final RequestLine line, final Headers headers,
                                                    final Content body) {
        return ResponseBuilder.notFound()
            .jsonBody(Json.createObjectBuilder().add(
                "error", String.format(
                    "\"%s\" could not be found",
                    new KeyLastPart(new KeyFromPath(line.uri().getPath())).get()
                )).build())
            .completedFuture();
    }
}
