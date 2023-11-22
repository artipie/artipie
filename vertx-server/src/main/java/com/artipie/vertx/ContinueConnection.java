/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.vertx;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.rs.RsStatus;
import io.vertx.reactivex.core.http.HttpServerResponse;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Connection which supports {@code 100 Continue} status code responses.
 * <p>
 * It sends continue status to client and then delegates other work to origin connection.
 * </p>
 * @since 0.2
 */
final class ContinueConnection implements Connection {

    /**
     * Vertx response output.
     */
    private final HttpServerResponse response;

    /**
     * Origin HTTP connection.
     */
    private final Connection origin;

    /**
     * Wraps origin connection with continue responses support.
     * @param response Vertx response output
     * @param origin Origin connection
     */
    ContinueConnection(final HttpServerResponse response, final Connection origin) {
        this.response = response;
        this.origin = origin;
    }

    @Override
    public CompletionStage<Void> accept(final RsStatus status, final Headers headers,
        final Publisher<ByteBuffer> body) {
        final CompletionStage<Void> res;
        if (status == RsStatus.CONTINUE) {
            this.response.writeContinue();
            res = CompletableFuture.completedFuture(null);
        } else {
            res = this.origin.accept(status, headers, body);
        }
        return res;
    }
}
