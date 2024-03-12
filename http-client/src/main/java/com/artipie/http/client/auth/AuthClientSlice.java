/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.auth;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.RemoteConfig;
import com.artipie.http.client.UriClientSlice;
import com.artipie.http.rs.RsStatus;
import com.google.common.collect.Iterables;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

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
     * Ctor.
     *
     * @param origin Origin slice.
     * @param auth Authenticator.
     */
    public AuthClientSlice(final Slice origin, final Authenticator auth) {
        this.origin = origin;
        this.auth = auth;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            new Content.From(body).asBytesFuture().thenApply(
                array -> Flowable.fromArray(ByteBuffer.wrap(Arrays.copyOf(array, array.length)))
            ).thenApply(
                copy -> connection -> this.auth.authenticate(Headers.EMPTY)
                    .thenCompose(
                        first -> this.origin.response(
                            line,
                            new Headers.From(headers, first),
                            copy
                        ).send(
                            (rsstatus, rsheaders, rsbody) -> {
                                if (rsstatus == RsStatus.UNAUTHORIZED) {
                                    return this.auth.authenticate(rsheaders)
                                        .thenCompose(
                                            authHeaders -> {
                                                if (Iterables.isEmpty(authHeaders)) {
                                                    return connection.accept(rsstatus, rsheaders, rsbody);
                                                }
                                                return this.origin.response(
                                                    line, new Headers.From(headers, authHeaders), copy
                                                ).send(connection);
                                            }
                                        );
                                }
                                return connection.accept(rsstatus, rsheaders, rsbody);
                            }
                        )
                    )
            )
        );
    }
}
