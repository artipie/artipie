/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;

/**
 * Slice with authorization.
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
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        return new AsyncResponse(
            this.auth.authenticate(headers, line).thenApply(
                result -> {
                    if (result.status() == AuthScheme.AuthStatus.AUTHENTICATED) {
                        if (this.control.allowed(result.user())) {
                            return this.origin.response(
                                line,
                                headers.copy().add(AuthzSlice.LOGIN_HDR, result.user().name()),
                                body
                            );
                        }
                        return new RsWithStatus(RsStatus.FORBIDDEN);
                    }
                    // The case of anonymous user
                    if (result.status() == AuthScheme.AuthStatus.NO_CREDENTIALS
                        && this.control.allowed(result.user())) {
                        return this.origin.response(
                            line,
                            headers.copy().add(AuthzSlice.LOGIN_HDR, result.user().name()),
                            body
                        );
                    }
                    return new RsWithHeaders(
                        new RsWithStatus(RsStatus.UNAUTHORIZED),
                        Headers.from(new WwwAuthenticate(result.challenge()))
                    );
                }
            )
        );
    }
}
