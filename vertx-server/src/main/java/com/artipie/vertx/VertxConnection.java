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
    public CompletionStage<Void> accept(RsStatus status, Headers headers, Content body) {
        this.rsp.setStatusCode(status.code());
        headers.stream()
            .forEach(h -> this.rsp.putHeader(h.getKey(), h.getValue()));

        final CompletableFuture<Void> promise = new CompletableFuture<>();
        final Flowable<Buffer> vpb = Flowable.fromPublisher(body)
            .map(VertxConnection::mapBuffer)
            .doOnError(promise::completeExceptionally);
        if (this.rsp.headers().contains("Content-Length")) {
            this.rsp.setChunked(false);
            vpb.doOnComplete(
                () -> {
                    this.rsp.end();
                    promise.complete(null);
                }
            ).forEach(this.rsp::write);
        } else {
            this.rsp.setChunked(true);
            vpb.doOnComplete(() -> promise.complete(null))
                .subscribe(this.rsp.toSubscriber());
        }
        return promise;
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
