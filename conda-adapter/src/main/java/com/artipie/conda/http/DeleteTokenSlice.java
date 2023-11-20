/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.conda.http.auth.TokenAuthScheme;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.Tokens;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Delete token slice.
 * <a href="https://api.anaconda.org/docs#/authentication/delete_authentications">Documentation</a>.
 * This slice checks if the token is valid and returns 201 if yes. Token itself is not removed
 * from the Artipie.
 * @since 0.5
 */
final class DeleteTokenSlice implements Slice {

    /**
     * Auth tokens.
     */
    private final Tokens tokens;

    /**
     * Ctor.
     * @param tokens Auth tokens
     */
    DeleteTokenSlice(final Tokens tokens) {
        this.tokens = tokens;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers, final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            CompletableFuture.supplyAsync(
                () -> new RqHeaders(headers, Authorization.NAME)
                    .stream().findFirst().map(Authorization::new)
                    .map(auth -> new Authorization.Token(auth.credentials()).token())
            ).thenCompose(
                tkn -> tkn.map(
                    item -> this.tokens.auth().user(item).<Response>thenApply(
                        user -> {
                            final RsStatus status;
                            if (user.isPresent()) {
                                status = RsStatus.CREATED;
                            } else {
                                status = RsStatus.BAD_REQUEST;
                            }
                            return new RsWithStatus(status);
                        }
                    )
                ).orElse(
                    CompletableFuture.completedFuture(
                        new RsWithHeaders(
                            new RsWithStatus(RsStatus.UNAUTHORIZED),
                            new Headers.From(new WwwAuthenticate(TokenAuthScheme.NAME))
                        )
                    )
                )
            )
        );
    }
}
