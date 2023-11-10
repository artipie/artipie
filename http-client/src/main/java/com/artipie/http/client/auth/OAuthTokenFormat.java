/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http.client.auth;

import java.io.ByteArrayInputStream;
import javax.json.Json;

/**
 * Authentication token response.
 * See <a href="https://tools.ietf.org/html/rfc6750#section-4">Example Access Token Response</a>
 *
 * @since 0.5
 */
final class OAuthTokenFormat implements TokenFormat {

    @Override
    public String token(final byte[] content) {
        return Json.createReader(new ByteArrayInputStream(content))
            .readObject()
            .getString("access_token");
    }
}
