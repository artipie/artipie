/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.async;

import com.artipie.http.Connection;
import com.artipie.http.Response;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.util.concurrent.CompletionStage;

/**
 * Async response from {@link CompletionStage}.
 * @since 0.6
 */
public final class AsyncResponse implements Response {

    /**
     * Source stage.
     */
    private final CompletionStage<? extends Response> future;

    /**
     * Response from {@link Single}.
     * @param single Single
     */
    public AsyncResponse(final Single<? extends Response> single) {
        this(single.to(SingleInterop.get()));
    }

    /**
     * Response from {@link CompletionStage}.
     * @param future Stage
     */
    public AsyncResponse(final CompletionStage<? extends Response> future) {
        this.future = future;
    }

    @Override
    public CompletionStage<Void> send(final Connection connection) {
        return this.future.thenCompose(rsp -> rsp.send(connection));
    }

    @Override
    public String toString() {
        return String.format(
            "(%s: %s)",
            this.getClass().getSimpleName(),
            this.future.toString()
        );
    }
}
