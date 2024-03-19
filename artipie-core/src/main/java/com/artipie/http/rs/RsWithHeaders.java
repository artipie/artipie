/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Response with additional headers.
 *
 * @since 0.7
 */
public final class RsWithHeaders implements Response {

    /**
     * Origin response.
     */
    private final Response origin;

    /**
     * Headers.
     */
    private final Headers headers;

    /**
     * Should header value be replaced if already exist? False by default.
     */
    private final boolean override;

    /**
     * Ctor.
     *
     * @param origin Response
     * @param headers Headers
     * @param override Should header value be replaced if already exist?
     */
    public RsWithHeaders(final Response origin, final Headers headers, final boolean override) {
        this.origin = origin;
        this.headers = headers;
        this.override = override;
    }

    /**
     * Ctor.
     *
     * @param origin Response
     * @param headers Headers
     */
    public RsWithHeaders(final Response origin, final Headers headers) {
        this(origin, headers, false);
    }

    /**
     * Ctor.
     *
     * @param origin Origin response.
     * @param headers Headers
     */
    @SafeVarargs
    public RsWithHeaders(final Response origin, final Map.Entry<String, String>... headers) {
        this(origin, Headers.from(headers));
    }

    /**
     * Ctor.
     *
     * @param origin Origin response.
     * @param name Name of header.
     * @param value Value of header.
     */
    public RsWithHeaders(final Response origin, final String name, final String value) {
        this(origin, Headers.from(name, value));
    }

    @Override
    public CompletionStage<Void> send(final Connection con) {
        return this.origin.send(new ConWithHeaders(con, this.headers, this.override));
    }

    /**
     * Connection with additional headers.
     * @since 0.3
     */
    private static final class ConWithHeaders implements Connection {

        /**
         * Origin connection.
         */
        private final Connection origin;

        /**
         * Additional headers.
         */
        private final Headers headers;

        /**
         * Should header value be replaced if already exist?
         */
        private final boolean override;

        /**
         * Ctor.
         *
         * @param origin Connection
         * @param headers Headers
         * @param override Should header value be replaced if already exist?
         */
        private ConWithHeaders(Connection origin, Headers headers, boolean override) {
            this.origin = origin;
            this.headers = headers;
            this.override = override;
        }

        @Override
        public CompletionStage<Void> accept(
            final RsStatus status,
            final Headers hrs,
            final Content body
        ) {
            final Headers res;
            if (this.override) {
                res = this.headers.copy();
                hrs.forEach(
                    item -> {
                        if (res.stream()
                            .noneMatch(val -> val.getKey().equalsIgnoreCase(item.getKey()))
                        ) {
                            res.add(item);
                        }
                    }
                );
            } else {
                res = this.headers.copy().addAll(hrs);
            }
            return this.origin.accept(status, res, body);
        }
    }
}
