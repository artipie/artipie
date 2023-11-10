/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;

/**
 * Absent resource, sends HTTP 404 Not Found response to every request.
 *
 * @since 0.1
 */
public final class Absent implements Resource {

    @Override
    public Response get(final Headers headers) {
        return new RsWithStatus(RsStatus.NOT_FOUND);
    }

    @Override
    public Response put(
        final Headers headers,
        final Publisher<ByteBuffer> body) {
        return new RsWithStatus(RsStatus.NOT_FOUND);
    }
}
