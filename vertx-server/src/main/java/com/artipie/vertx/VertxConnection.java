/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.vertx;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Vertx connection accepts Artipie response and send it to {@link HttpServerResponse}.
 * @since 0.2
 */
final class VertxConnection implements Connection {

    /**
     * Vertx server response output.
     */
    private final HttpServerResponse rsp;

    /**
     * New connection for response.
     * @param rsp Response output
     */
    VertxConnection(final HttpServerResponse rsp) {
        this.rsp = rsp;
    }

    @Override
    public CompletionStage<Void> accept(
        final RsStatus status,
        final Headers headers,
        final Content body
    ) {
        final int code = Integer.parseInt(status.code());
        this.rsp.setStatusCode(code);
        for (final Map.Entry<String, String> header : headers) {
            this.rsp.putHeader(header.getKey(), header.getValue());
        }
        final CompletableFuture<HttpServerResponse> promise = new CompletableFuture<>();
        final Flowable<Buffer> vpb = Flowable.fromPublisher(body)
            .map(VertxConnection::mapBuffer)
            .doOnError(promise::completeExceptionally);
        if (this.rsp.headers().contains("Content-Length")) {
            this.rsp.setChunked(false);
            vpb.doOnComplete(
                () -> {
                    this.rsp.end();
                    promise.complete(this.rsp);
                }
            ).forEach(this.rsp::write);
        } else {
            this.rsp.setChunked(true);
            vpb.doOnComplete(() -> promise.complete(this.rsp))
                .subscribe(this.rsp.toSubscriber());
        }
        return promise.thenCompose(ignored -> CompletableFuture.allOf());
    }

    /**
     * Map {@link ByteBuffer} to {@link Buffer}.
     * @param buffer Java byte buffer
     * @return Vertx buffer
     */
    private static Buffer mapBuffer(final ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return Buffer.buffer(bytes);
    }
}
