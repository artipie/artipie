/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.http.rq.RequestLine;

/**
 * Arti-pie slice.
 * <p>
 * Slice is a part of Artipie server.
 * Each Artipie adapter implements this interface to expose
 * repository HTTP API.
 * Artipie main module joins all slices together into solid web server.
 */
public interface Slice {

    /**
     * Respond to a http request.
     * @param line The request line
     * @param headers The request headers
     * @param body The request body
     * @return The response.
     */
    Response response(RequestLine line, Headers headers, Content body);

    /**
     * SliceWrap is a simple decorative envelope for Slice.
     */
    abstract class Wrap implements Slice {

        /**
         * Origin slice.
         */
        private final Slice slice;

        /**
         * @param slice Slice.
         */
        protected Wrap(final Slice slice) {
            this.slice = slice;
        }

        @Override
        public final Response response(RequestLine line, Headers headers, Content body) {
            return this.slice.response(line, headers, body);
        }
    }
}
