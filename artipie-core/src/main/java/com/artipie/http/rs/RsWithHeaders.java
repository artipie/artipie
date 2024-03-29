/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;

import java.util.concurrent.CompletionStage;

/**
 * Response with additional headers.
 */
@Deprecated
public final class RsWithHeaders implements Response {

    private final Response origin;
    private final Headers headers;

    /**
     * @param origin Response
     * @param headers Headers
     */
    public RsWithHeaders(final Response origin, final Headers headers) {
        this.origin = origin;
        this.headers = headers;
    }

    @Override
    public CompletionStage<Void> send(final Connection con) {
        return this.origin.send(new ConWithHeaders(con, this.headers));
    }

    private static final class ConWithHeaders implements Connection {

        private final Connection origin;
        private final Headers headers;

        private ConWithHeaders(Connection origin, Headers headers) {
            this.origin = origin;
            this.headers = headers;
        }

        @Override
        public CompletionStage<Void> accept(RsStatus status, Headers hrs, Content body) {
            final Headers res = this.headers.copy().addAll(hrs);
            return this.origin.accept(status, res, body);
        }
    }
}
