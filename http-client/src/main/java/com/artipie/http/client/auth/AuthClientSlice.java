/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.auth;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.RemoteConfig;
import com.artipie.http.client.UriClientSlice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.RsStatus;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Slice augmenting requests with authentication when needed.
 */
public final class AuthClientSlice implements Slice {

    public static AuthClientSlice withClientSlice(ClientSlices client, RemoteConfig cfg) {
        return new AuthClientSlice(
            client.from(cfg.uri().toString()),
            GenericAuthenticator.create(client, cfg.username(), cfg.pwd())
        );
    }

    public static AuthClientSlice withUriClientSlice(ClientSlices client, RemoteConfig cfg) {
        return new AuthClientSlice(
            new UriClientSlice(client, cfg.uri()),
            GenericAuthenticator.create(client, cfg.username(), cfg.pwd())
        );
    }

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Authenticator.
     */
    private final Authenticator auth;

    /**
     * @param origin Origin slice.
     * @param auth Authenticator.
     */
    public AuthClientSlice(Slice origin, Authenticator auth) {
        this.origin = origin;
        this.auth = auth;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
        return body.asBytesFuture()
            .thenApply(data -> {
                Content copyContent = new Content.From(Arrays.copyOf(data, data.length));
                return this.auth.authenticate(Headers.EMPTY)
                    .toCompletableFuture()
                    .thenCompose(
                        authFirst -> this.origin.response(
                                line, headers.copy().addAll(authFirst), copyContent
                            ).thenApply(
                                response -> {
                                    if (response.status() == RsStatus.UNAUTHORIZED) {
                                        return this.auth.authenticate(response.headers())
                                            .thenCompose(
                                                authSecond -> {
                                                    if (authSecond.isEmpty()) {
                                                        return CompletableFuture.completedFuture(response);
                                                    }
                                                    return this.origin.response(
                                                        line, headers.copy().addAll(authSecond), copyContent
                                                    );
                                                }
                                            );
                                    }
                                    return CompletableFuture.completedFuture(response);
                                })
                            .thenCompose(Function.identity())
                    );
            }).thenCompose(Function.identity());
    }
}
