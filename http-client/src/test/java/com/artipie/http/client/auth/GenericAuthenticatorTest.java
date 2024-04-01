/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.auth;

import com.artipie.http.Headers;
import com.artipie.http.client.FakeClientSlices;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.ResponseBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Tests for {@link GenericAuthenticator}.
 */
class GenericAuthenticatorTest {

    @Test
    void shouldProduceNothingWhenNoAuthRequested() {
        MatcherAssert.assertThat(
            new GenericAuthenticator(
                new FakeClientSlices((line, headers, body) -> CompletableFuture.completedFuture(
                    ResponseBuilder.ok().build())),
                "alice",
                "qwerty"
            ).authenticate(Headers.EMPTY).toCompletableFuture().join(),
            new IsEqual<>(Headers.EMPTY)
        );
    }

    @Test
    void shouldProduceBasicHeaderWhenRequested() {
        MatcherAssert.assertThat(
            StreamSupport.stream(
                new GenericAuthenticator(
                    new FakeClientSlices((line, headers, body) -> CompletableFuture.completedFuture(
                        ResponseBuilder.ok().build())),
                    "Aladdin",
                    "open sesame"
                ).authenticate(
                    Headers.from(new WwwAuthenticate("Basic"))
                ).toCompletableFuture().join().spliterator(),
                false
            ).map(Map.Entry::getKey).collect(Collectors.toList()),
            Matchers.contains(Authorization.NAME)
        );
    }

    @Test
    void shouldProduceBearerHeaderWhenRequested() {
        MatcherAssert.assertThat(
            StreamSupport.stream(
                new GenericAuthenticator(
                    new FakeClientSlices(
                        (line, headers, body) -> CompletableFuture.completedFuture(ResponseBuilder.ok()
                            .jsonBody("{\"access_token\":\"mF_9.B5f-4.1JqM\"}")
                            .build())
                    ),
                    "bob",
                    "12345"
                ).authenticate(
                    Headers.from(new WwwAuthenticate("Bearer realm=\"https://artipie.com\""))
                ).toCompletableFuture().join().spliterator(),
                false
            ).map(Map.Entry::getKey).collect(Collectors.toList()),
            Matchers.contains(Authorization.NAME)
        );
    }
}
