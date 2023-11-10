/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.http.auth;

import com.artipie.http.Slice;
import com.artipie.http.auth.AuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.auth.TokenAuthentication;

/**
 * Token authentication slice.
 * @since 0.5
 */
public final class TokenAuthSlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param origin Origin slice
     * @param control Operation control
     * @param tokens Token authentication
     */
    public TokenAuthSlice(
        final Slice origin, final OperationControl control, final TokenAuthentication tokens
    ) {
        super(new AuthzSlice(origin, new TokenAuthScheme(new TokenAuth(tokens)), control));
    }
}
