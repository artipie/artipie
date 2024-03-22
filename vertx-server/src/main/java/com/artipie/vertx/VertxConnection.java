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
 */
final class VertxConnection implements Connection {

    /**
     * Vertx server response output.
     */
    private final HttpServerResponse response;

    /**
     * New connection for response.
     * @param response Response output
     */
    VertxConnection(final HttpServerResponse response) {
        this.response = response;
    }

    @Override
    public CompletionStage<Void> accept(RsStatus status, Headers headers, Content body) {
        final CompletableFuture<Void> promise = new CompletableFuture<>();
        if(status == RsStatus.CONTINUE){
            this.response.writeContinue();
            return CompletableFuture.completedFuture(null);
        }

        this.response.setStatusCode(status.code());
        headers.stream().forEach(h -> this.response.putHeader(h.getKey(), h.getValue()));

        final Flowable<Buffer> vpb = Flowable.fromPublisher(body)
            .map(VertxConnection::mapBuffer)
            .doOnError(promise::completeExceptionally);
        if (this.response.headers().contains("Content-Length")) {
            this.response.setChunked(false);
            vpb.doOnComplete(
                () -> {
                    this.response.end();
                    promise.complete(null);
                }
            ).forEach(this.response::write);
        } else {
            this.response.setChunked(true);
            vpb.doOnComplete(() -> promise.complete(null))
                .subscribe(this.response.toSubscriber());
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
