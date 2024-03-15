/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rq.RequestLine;
import com.artipie.security.policy.Policy;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice that implements authorization for {@link ScopeSlice}.
 *
 * @since 0.11
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
     * Ctor.
     *
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
    public Response response(
        final RequestLine line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AuthzSlice(
            this.origin,
            this.auth,
            new OperationControl(this.policy, this.origin.permission(line, this.name))
        ).response(line, headers, body);
    }
}
