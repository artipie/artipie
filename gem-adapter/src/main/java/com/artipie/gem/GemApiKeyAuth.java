/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RqHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.codec.binary.Base64;

/**
 * {@link AuthScheme} implementation for gem api key decoding.
 * @since 0.6
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
        final Iterable<Map.Entry<String, String>> headers,
        final String header
    ) {
        return new RqHeaders(headers, Authorization.NAME).stream()
            .findFirst()
            .map(
                str -> {
                    final CompletionStage<Result> res;
                    if (str.startsWith(BasicAuthScheme.NAME)) {
                        res = new BasicAuthScheme(this.auth).authenticate(headers);
                    } else {
                        res = CompletableFuture.completedFuture(
                            Optional.of(str)
                                .map(item -> item.getBytes(StandardCharsets.UTF_8))
                                .map(Base64::decodeBase64)
                                .map(String::new)
                                .map(dec -> dec.split(":"))
                                .flatMap(
                                    cred -> this.auth.user(cred[0].trim(), cred[1].trim())
                                )
                                .<Result>map(Success::new)
                                .orElseGet(Failure::new)
                        );
                    }
                    return res;
                }
            )
            .get();
    }

    /**
     * Successful result with authenticated user.
     *
     * @since 0.5.4
     */
    private static class Success implements AuthScheme.Result {

        /**
         * Authenticated user.
         */
        private final AuthUser usr;

        /**
         * Ctor.
         *
         * @param user Authenticated user.
         */
        Success(final AuthUser user) {
            this.usr = user;
        }

        @Override
        public Optional<AuthUser> user() {
            return Optional.of(this.usr);
        }

        @Override
        public String challenge() {
            return "";
        }
    }

    /**
     * Failed result without authenticated user.
     *
     * @since 0.5.4
     */
    private static class Failure implements AuthScheme.Result {

        @Override
        public Optional<AuthUser> user() {
            return Optional.empty();
        }

        @Override
        public String challenge() {
            return "";
        }
    }
}
