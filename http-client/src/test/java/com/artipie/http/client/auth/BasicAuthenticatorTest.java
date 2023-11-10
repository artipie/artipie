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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Authenticator} instances.
 *
 * @since 0.3
 */
class BasicAuthenticatorTest {

    @Test
    void shouldProduceBasicHeader() {
        MatcherAssert.assertThat(
            StreamSupport.stream(
                new BasicAuthenticator("Aladdin", "open sesame")
                    .authenticate(Headers.EMPTY)
                    .toCompletableFuture().join()
                    .spliterator(),
                false
            ).map(Header::new).collect(Collectors.toList()),
            Matchers.contains(new Header("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="))
        );
    }
}
