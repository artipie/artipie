/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.auth;

import com.artipie.http.Headers;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.UriClientSlice;
import com.artipie.http.client.misc.PublisherAs;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import io.reactivex.Flowable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Bearer authenticator using specified authenticator and format to get required token.
 *
 * @since 0.4
 */
public final class BearerAuthenticator implements Authenticator {

    /**
     * Client slices.
     */
    private final ClientSlices client;

    /**
     * Token format.
     */
    private final TokenFormat format;

    /**
     * Token request authenticator.
     */
    private final Authenticator auth;

    /**
     * Ctor.
     *
     * @param client Client slices.
     * @param format Token format.
     * @param auth Token request authenticator.
     */
    public BearerAuthenticator(
        final ClientSlices client,
        final TokenFormat format,
        final Authenticator auth
    ) {
        this.client = client;
        this.format = format;
        this.auth = auth;
    }

    @Override
    public CompletionStage<Headers> authenticate(final Headers headers) {
        return this.authenticate(new WwwAuthenticate(headers)).thenApply(Headers.From::new);
    }

    /**
     * Creates 'Authorization' header using requirements from 'WWW-Authenticate'.
     *
     * @param header WWW-Authenticate header.
     * @return Authorization header.
     */
    private CompletionStage<Authorization.Bearer> authenticate(final WwwAuthenticate header) {
        final URI realm;
        try {
            realm = new URI(header.realm());
        } catch (final URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        final String query = header.params().stream()
            .filter(param -> !param.name().equals("realm"))
            .map(param -> String.format("%s=%s", param.name(), param.value()))
            .collect(Collectors.joining("&"));
        final CompletableFuture<String> promise = new CompletableFuture<>();
        return new AuthClientSlice(new UriClientSlice(this.client, realm), this.auth).response(
            new RequestLine(RqMethod.GET, String.format("?%s", query)).toString(),
            Headers.EMPTY,
            Flowable.empty()
        ).send(
            (status, headers, body) -> new PublisherAs(body).bytes()
                .thenApply(this.format::token)
                .thenCompose(
                    token -> {
                        promise.complete(token);
                        return CompletableFuture.allOf();
                    }
                )
        ).thenCompose(ignored -> promise).thenApply(Authorization.Bearer::new);
    }
}
