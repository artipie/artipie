/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;

import java.util.regex.Pattern;

/**
 * This slice work with documentations.
 */
public final class DocsSlice implements Slice {
    /**
     * Pattern for docs.
     */
    static final Pattern DOCS_PTRN = Pattern.compile("^/(.*)/docs$");

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        return new RsWithStatus(RsStatus.OK);
    }
}
