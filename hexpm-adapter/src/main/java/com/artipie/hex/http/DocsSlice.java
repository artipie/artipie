/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * This slice work with documentations.
 * @since 0.1
 */
public final class DocsSlice implements Slice {
    /**
     * Pattern for docs.
     */
    static final Pattern DOCS_PTRN = Pattern.compile("^/(.*)/docs$");

    @Override
    public Response response(
        final RequestLine line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new RsWithStatus(RsStatus.OK);
    }
}
