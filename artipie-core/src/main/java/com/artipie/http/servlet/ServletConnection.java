/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.servlet;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.rs.RsStatus;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.servlet.http.HttpServletResponse;
import org.cqfn.rio.WriteGreed;
import org.cqfn.rio.stream.ReactiveOutputStream;
import org.reactivestreams.Publisher;

/**
 * Connection implementation with servlet response as a back-end.
 * @since 0.18
 */
final class ServletConnection implements Connection {

    /**
     * Servlet response.
     */
    private final HttpServletResponse rsp;

    /**
     * New Artipie connection with servlet response back-end.
     * @param rsp Servlet response
     */
    ServletConnection(final HttpServletResponse rsp) {
        this.rsp = rsp;
    }

    // @checkstyle ReturnCountCheck (10 lines)
    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public CompletionStage<Void> accept(final RsStatus status,
        final Headers headers, final Publisher<ByteBuffer> body) {
        this.rsp.setStatus(Integer.parseInt(status.code()));
        headers.forEach(kv -> this.rsp.setHeader(kv.getKey(), kv.getValue()));
        try {
            return new ReactiveOutputStream(this.rsp.getOutputStream())
                .write(body, WriteGreed.SYSTEM.adaptive());
        } catch (final IOException iex) {
            final CompletableFuture<Void> failure = new CompletableFuture<>();
            failure.completeExceptionally(new CompletionException(iex));
            return failure;
        }
    }
}
