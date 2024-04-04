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
     * Docker registry name.
     */
    private final String registryName;

    /**
     * @param origin Origin slice.
     * @param auth Authentication scheme.
     * @param policy Access permissions.
     * @param registryName Docker registry name.
     */
    AuthScopeSlice(ScopeSlice origin, AuthScheme auth, Policy<?> policy, String registryName) {
        this.origin = origin;
        this.auth = auth;
        this.policy = policy;
        this.registryName = registryName;
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        return new AuthzSlice(
            this.origin,
            this.auth,
            new OperationControl(this.policy, this.origin.permission(line, this.registryName))
        ).response(line, headers, body);
    }
}
