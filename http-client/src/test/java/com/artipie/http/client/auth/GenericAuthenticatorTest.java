/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http.client.auth;

import com.artipie.http.Headers;
import com.artipie.http.client.FakeClientSlices;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GenericAuthenticator}.
 *
 * @since 0.3
 */
class GenericAuthenticatorTest {

    @Test
    void shouldProduceNothingWhenNoAuthRequested() {
        MatcherAssert.assertThat(
            new GenericAuthenticator(
                new FakeClientSlices((line, headers, body) -> StandardRs.OK),
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
                    new FakeClientSlices((line, headers, body) -> StandardRs.OK),
                    "Aladdin",
                    "open sesame"
                ).authenticate(
                    new Headers.From(new WwwAuthenticate("Basic"))
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
                        (line, headers, body) -> new RsWithBody(
                            StandardRs.EMPTY,
                            "{\"access_token\":\"mF_9.B5f-4.1JqM\"}".getBytes()
                        )
                    ),
                    "bob",
                    "12345"
                ).authenticate(
                    new Headers.From(new WwwAuthenticate("Bearer realm=\"https://artipie.com\""))
                ).toCompletableFuture().join().spliterator(),
                false
            ).map(Map.Entry::getKey).collect(Collectors.toList()),
            Matchers.contains(Authorization.NAME)
        );
    }
}
