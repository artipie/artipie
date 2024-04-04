/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import com.artipie.http.Headers;
import com.artipie.http.auth.AuthzSlice;
import com.artipie.http.rq.RqHeaders;
import com.artipie.scheduling.ArtifactEvent;

import java.util.Map;

/**
 * Login header.
 */
public final class Login extends Header {

    /**
     * @param headers Header.
     */
    public Login(final Map.Entry<String, String> headers) {
        this(
            new RqHeaders(Headers.from(headers), AuthzSlice.LOGIN_HDR)
                .stream().findFirst().orElse(ArtifactEvent.DEF_OWNER)
        );
    }

    /**
     * @param headers Header.
     */
    public Login(final Headers headers) {
        this(headers.find(AuthzSlice.LOGIN_HDR).stream()
            .findFirst()
            .map(Header::getValue)
            .orElse(ArtifactEvent.DEF_OWNER)
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
