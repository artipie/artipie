/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.http.Response;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import org.reactivestreams.Publisher;

/**
 * Route path.
 * @since 0.10
 */
public interface RtPath {
    /**
     * Try respond.
     * @param line Request line
     * @param headers Headers
     * @param body Body
     * @return Response if passed routing rule
     */
    Optional<Response> response(
        String line,
        Iterable<Map.Entry<String, String>> headers,
        Publisher<ByteBuffer> body
    );
}
