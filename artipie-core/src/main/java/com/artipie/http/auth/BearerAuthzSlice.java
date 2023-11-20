/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.http.Slice;

/**
 * Slice with bearer token authorization.
 * @since 1.2
 */
public final class BearerAuthzSlice extends Slice.Wrap {

    /**
     * Creates bearer auth slice with {@link BearerAuthScheme} and empty challenge params.
     * @param origin Origin slice
     * @param auth Authorization
     * @param control Access control by permission
     */
    public BearerAuthzSlice(final Slice origin, final TokenAuthentication auth,
        final OperationControl control) {
        super(new AuthzSlice(origin, new BearerAuthScheme(auth, ""), control));
    }

    /**
     * Ctor.
     * @param origin Origin slice
     * @param scheme Bearer authentication scheme
     * @param control Access control by permission
     */
    public BearerAuthzSlice(final Slice origin, final BearerAuthScheme scheme,
        final OperationControl control) {
        super(new AuthzSlice(origin, scheme, control));
    }
}
