/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.ext.KeyLastPart;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.common.RsJson;
import com.artipie.http.slice.KeyFromPath;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import org.reactivestreams.Publisher;

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
    public Response response(final RequestLine line, final Headers headers,
                             final Publisher<ByteBuffer> body) {
        return new RsJson(
            RsStatus.NOT_FOUND,
            () -> Json.createObjectBuilder().add(
                "error", String.format(
                    "\"%s\" could not be found",
                    new KeyLastPart(new KeyFromPath(line.uri().getPath())).get()
                )
            ).build(),
            StandardCharsets.UTF_8
        );
    }
}
