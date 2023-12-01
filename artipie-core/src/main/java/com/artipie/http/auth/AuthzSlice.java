/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice with authorization.
 *
 * @since 1.2
 */
public final class AuthzSlice implements Slice {

    /**
     * Header for artipie login.
     */
    public static final String LOGIN_HDR = "artipie_login";

    /**
     * Origin.
     */
    private final Slice origin;

    /**
     * Authentication scheme.
     */
    private final AuthScheme auth;

    /**
     * Access control by permission.
     */
    private final OperationControl control;

    /**
     * Ctor.
     *
     * @param origin Origin slice.
     * @param auth Authentication scheme.
     * @param control Access control by permission.
     */
    public AuthzSlice(final Slice origin, final AuthScheme auth, final OperationControl control) {
        this.origin = origin;
        this.auth = auth;
        this.control = control;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            this.auth.authenticate(headers, line).thenApply(
                result -> result.user().map(
                    usr -> {
                        final Response rsp;
                        if (this.control.allowed(usr)) {
                            rsp = this.origin.response(
                                line,
                                new Headers.From(headers, AuthzSlice.LOGIN_HDR, usr.name()),
                                body
                            );
                        } else {
                            rsp = new RsWithStatus(RsStatus.FORBIDDEN);
                        }
                        return rsp;
                    }
                ).orElseGet(
                    () -> new RsWithHeaders(
                        new RsWithStatus(RsStatus.UNAUTHORIZED),
                        new Headers.From(new WwwAuthenticate(result.challenge()))
                    )
                )
            )
        );
    }
}
