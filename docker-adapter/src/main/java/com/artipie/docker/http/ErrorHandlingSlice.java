/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.error.DockerError;
import com.artipie.docker.error.UnsupportedError;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/**
 * Slice that handles exceptions in origin slice by sending well-formed error responses.
 */
final class ErrorHandlingSlice implements Slice {

    private final Slice origin;

    /**
     * @param origin Origin.
     */
    ErrorHandlingSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public CompletableFuture<Response> response(
        RequestLine line, Headers headers, Content body
    ) {
        try {
            return this.origin.response(line, headers, body)
                .handle((response, error) -> {
                    CompletableFuture<Response> res;
                    if (error != null) {
                        res = handle(error)
                            .map(CompletableFuture::completedFuture)
                            .orElseGet(() -> CompletableFuture.failedFuture(error));
                        } else {
                        res = CompletableFuture.completedFuture(response);
                        }
                    return res;
                    }
                ).thenCompose(Function.identity());
        } catch (Exception error) {
            return handle(error)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> CompletableFuture.failedFuture(error));
        }
    }

    /**
     * Translates throwable to error response.
     *
     * @param throwable Throwable to translate.
     * @return Result response, empty that throwable cannot be handled.
     */
    private static Optional<Response> handle(final Throwable throwable) {
        if (throwable instanceof DockerError error) {
            return Optional.of(ResponseBuilder.badRequest().jsonBody(error.json()).build());
        }
        if (throwable instanceof UnsupportedOperationException) {
            return Optional.of(
                ResponseBuilder.methodNotAllowed().jsonBody(new UnsupportedError().json()).build()
            );
        }
        if (throwable instanceof CompletionException) {
            return Optional.ofNullable(throwable.getCause()).flatMap(ErrorHandlingSlice::handle);
        }
        return Optional.empty();
    }
}
