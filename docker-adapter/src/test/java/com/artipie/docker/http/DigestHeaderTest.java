/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Digest;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link DigestHeader}.
 *
 * @since 0.2
 */
public final class DigestHeaderTest {

    @Test
    void shouldHaveExpectedNameAndValue() {
        final DigestHeader header = new DigestHeader(
            new Digest.Sha256(
                "6c3c624b58dbbcd3c0dd82b4c53f04194d1247c6eebdaab7c610cf7d66709b3b"
            )
        );
        MatcherAssert.assertThat(
            header.getKey(),
            new IsEqual<>("Docker-Content-Digest")
        );
        MatcherAssert.assertThat(
            header.getValue(),
            new IsEqual<>("sha256:6c3c624b58dbbcd3c0dd82b4c53f04194d1247c6eebdaab7c610cf7d66709b3b")
        );
    }

    @Test
    void shouldExtractValueFromHeaders() {
        final String digest = "sha256:123";
        final DigestHeader header = new DigestHeader(
            new Headers.From(
                new Header("Content-Type", "application/octet-stream"),
                new Header("docker-content-digest", digest),
                new Header("X-Something", "Some Value")
            )
        );
        MatcherAssert.assertThat(header.value().string(), new IsEqual<>(digest));
    }

    @Test
    void shouldFailToExtractValueFromEmptyHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new DigestHeader(Headers.EMPTY).value()
        );
    }
}
