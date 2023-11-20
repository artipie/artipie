/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * The http connection.
 * @since 0.1
 */
public interface Connection {

    /**
     * Respond on connection.
     * @param status The http status code.
     * @param headers The http response headers.
     * @param body The http response body.
     * @return Completion stage for accepting HTTP response.
     */
    CompletionStage<Void> accept(RsStatus status, Headers headers, Publisher<ByteBuffer> body);

    /**
     * Respond on connection.
     * @param status The http status code.
     * @param headers The http response headers.
     * @param body The http response body.
     * @return Completion stage for accepting HTTP response.
     * @deprecated Use {@link Connection#accept(RsStatus, Headers, Publisher)}.
     */
    @Deprecated
    default CompletionStage<Void> accept(
        RsStatus status,
        Iterable<Map.Entry<String, String>> headers,
        Publisher<ByteBuffer> body
    ) {
        return this.accept(status, new Headers.From(headers), body);
    }
}
