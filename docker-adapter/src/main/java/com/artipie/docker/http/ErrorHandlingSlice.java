/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.FailedCompletionStage;
import com.artipie.docker.error.DockerError;
import com.artipie.docker.error.UnsupportedError;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * Slice that handles exceptions in origin slice by sending well-formed error responses.
 *
 * @since 0.5
 */
final class ErrorHandlingSlice implements Slice {

    /**
     * Origin.
     */
    private final Slice origin;

    /**
     * Ctor.
     *
     * @param origin Origin.
     */
    ErrorHandlingSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        Response response;
        try {
            final Response original = this.origin.response(line, headers, body);
            response = connection -> {
                CompletionStage<Void> sent;
                try {
                    sent = original.send(connection);
                    // @checkstyle IllegalCatchCheck (1 line)
                } catch (final RuntimeException ex) {
                    sent = handle(ex).map(rsp -> rsp.send(connection)).orElseThrow(() -> ex);
                }
                return sent.handle(
                    (nothing, throwable) -> {
                        final CompletionStage<Void> result;
                        if (throwable == null) {
                            result = CompletableFuture.completedFuture(nothing);
                        } else {
                            result = handle(throwable)
                                .map(rsp -> rsp.send(connection))
                                .orElseGet(() -> new FailedCompletionStage<>(throwable));
                        }
                        return result;
                    }
                ).thenCompose(Function.identity());
            };
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final RuntimeException ex) {
            response = handle(ex).orElseThrow(() -> ex);
        }
        return response;
    }

    /**
     * Translates throwable to error response.
     *
     * @param throwable Throwable to translate.
     * @return Result response, empty that throwable cannot be handled.
     * @checkstyle ReturnCountCheck (3 lines)
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    private static Optional<Response> handle(final Throwable throwable) {
        if (throwable instanceof DockerError) {
            return Optional.of(
                new ErrorsResponse(RsStatus.BAD_REQUEST, (DockerError) throwable)
            );
        }
        if (throwable instanceof UnsupportedOperationException) {
            return Optional.of(
                new ErrorsResponse(RsStatus.METHOD_NOT_ALLOWED, new UnsupportedError())
            );
        }
        if (throwable instanceof CompletionException) {
            return Optional.ofNullable(throwable.getCause()).flatMap(ErrorHandlingSlice::handle);
        }
        return Optional.empty();
    }
}
