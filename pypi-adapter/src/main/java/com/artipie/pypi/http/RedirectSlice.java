/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.pypi.NormalizedProjectName;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice to redirect to normalized url.
 * @since 0.6
 */
public final class RedirectSlice implements Slice {

    /**
     * Full path header name.
     */
    private static final String HDR_FULL_PATH = "X-FullPath";

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final String rqline = new RequestLineFrom(line).uri().toString();
        final String last = rqline.split("/")[rqline.split("/").length - 1];
        return new AsyncResponse(
            Single.fromCallable(() -> last)
                .map(name -> new NormalizedProjectName.Simple(name).value())
                .map(
                    normalized -> new RqHeaders(headers, RedirectSlice.HDR_FULL_PATH).stream()
                    .findFirst()
                    .orElse(rqline).replaceAll(String.format("(%s\\/?)$", last), normalized)
                )
                .map(
                    url -> new RsWithHeaders(
                        new RsWithStatus(RsStatus.MOVED_PERMANENTLY),
                        new Headers.From("Location", url)
                    )
                )
        );
    }
}
