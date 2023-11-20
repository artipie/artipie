/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import com.artipie.http.Headers;
import com.artipie.http.auth.AuthzSlice;
import com.artipie.http.rq.RqHeaders;
import com.artipie.scheduling.ArtifactEvent;
import java.util.Map;

/**
 * Login header.
 * @since 1.13
 */
public final class Login extends Header.Wrap {

    /**
     * Ctor.
     *
     * @param headers Header.
     */
    public Login(final Map.Entry<String, String> headers) {
        this(
            new RqHeaders(new Headers.From(headers), AuthzSlice.LOGIN_HDR)
                .stream().findFirst().orElse(ArtifactEvent.DEF_OWNER)
        );
    }

    /**
     * Ctor.
     *
     * @param headers Header.
     */
    public Login(final Headers headers) {
        this(
            new RqHeaders(headers, AuthzSlice.LOGIN_HDR)
                .stream().findFirst().orElse(ArtifactEvent.DEF_OWNER)
        );
    }

    /**
     * Ctor.
     * @param value Header value
     */
    public Login(final String value) {
        super(new Header(AuthzSlice.LOGIN_HDR, value));
    }
}
