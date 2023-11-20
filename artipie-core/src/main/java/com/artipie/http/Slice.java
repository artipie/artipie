/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http;

import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Arti-pie slice.
 * <p>
 * Slice is a part of Artipie server.
 * Each Artipie adapter implements this interface to expose
 * repository HTTP API.
 * Artipie main module joins all slices together into solid web server.
 * </p>
 * @since 0.1
 */
public interface Slice {

    /**
     * Respond to a http request.
     * @param line The request line
     * @param headers The request headers
     * @param body The request body
     * @return The response.
     */
    Response response(
        String line,
        Iterable<Map.Entry<String, String>> headers,
        Publisher<ByteBuffer> body
    );

    /**
     * SliceWrap is a simple decorative envelope for Slice.
     *
     * @since 0.7
     */
    abstract class Wrap implements Slice {

        /**
         * Origin slice.
         */
        private final Slice slice;

        /**
         * Ctor.
         *
         * @param slice Slice.
         */
        protected Wrap(final Slice slice) {
            this.slice = slice;
        }

        @Override
        public final Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body) {
            return this.slice.response(line, headers, body);
        }
    }
}
