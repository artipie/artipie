/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.http.Headers;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * {@link AuthScheme} implementation for gem api key decoding.
 */
public final class GemApiKeyAuth implements AuthScheme {

    /**
     * Concrete implementation for User Identification.
     */
    private final Authentication auth;

    /**
     * Ctor.
     * @param auth Concrete implementation for User Identification.
     */
    public GemApiKeyAuth(final Authentication auth) {
        this.auth = auth;
    }

    @Override
    public CompletionStage<Result> authenticate(
        Headers headers, RequestLine line
    ) {
        return new RqHeaders(headers, Authorization.NAME).stream()
            .findFirst()
            .map(
                str -> {
                    final CompletionStage<Result> res;
                    if (str.startsWith(BasicAuthScheme.NAME)) {
                        res = new BasicAuthScheme(this.auth).authenticate(headers);
                    } else {
                        final String[] cred = new String(
                            Base64.decodeBase64(str.getBytes(StandardCharsets.UTF_8))
                        ).split(":");
                        final Optional<AuthUser> user = this.auth.user(
                            cred[0].trim(), cred[1].trim()
                        );
                        res = CompletableFuture.completedFuture(AuthScheme.result(user, ""));
                    }
                    return res;
                }
            )
            .orElse(
                CompletableFuture.completedFuture(
                    AuthScheme.result(AuthUser.ANONYMOUS, "")
                )
            );
    }
}
