/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rq.RequestLine;
import com.artipie.security.policy.Policy;

import java.util.concurrent.CompletableFuture;

/**
 * Slice that implements authorization for {@link ScopeSlice}.
 */
final class AuthScopeSlice implements Slice {

    /**
     * Origin.
     */
    private final ScopeSlice origin;

    /**
     * Authentication scheme.
     */
    private final AuthScheme auth;

    /**
     * Access permissions.
     */
    private final Policy<?> policy;

    /**
     * Artipie repository name.
     */
    private final String name;

    /**
     * @param origin Origin slice.
     * @param auth Authentication scheme.
     * @param policy Access permissions.
     * @param name Repository name
     */
    AuthScopeSlice(
        final ScopeSlice origin,
        final AuthScheme auth,
        final Policy<?> policy,
        final String name
    ) {
        this.origin = origin;
        this.auth = auth;
        this.policy = policy;
        this.name = name;
    }

    @Override
    public CompletableFuture<Response> response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        return new AuthzSlice(
            this.origin,
            this.auth,
            new OperationControl(this.policy, this.origin.permission(line, this.name))
        ).response(line, headers, body);
    }
}
