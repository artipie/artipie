/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http.client.auth;

import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Authenticator#ANONYMOUS}.
 *
 * @since 0.4
 */
class AuthenticatorAnonymousTest {

    @Test
    void shouldProduceEmptyHeader() {
        MatcherAssert.assertThat(
            StreamSupport.stream(
                Authenticator.ANONYMOUS.authenticate(Headers.EMPTY)
                    .toCompletableFuture().join()
                    .spliterator(),
                false
            ).map(Header::new).collect(Collectors.toList()),
            new IsEmptyCollection<>()
        );
    }
}
