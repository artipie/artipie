/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http.client.auth;

/**
 * Format of Access Token used for Bearer authentication.
 * See <a href="https://tools.ietf.org/html/rfc6750#section-1.3">Overview</a>
 *
 * @since 0.5
 */
public interface TokenFormat {

    /**
     * Reads token string from bytes.
     *
     * @param bytes Bytes.
     * @return Token string.
     */
    String token(byte[] bytes);
}
