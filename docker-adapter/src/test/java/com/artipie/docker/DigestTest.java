/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.docker;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Digest}.
 *
 * @since 0.1
 */
public final class DigestTest {
    @Test
    void parsesValidString() {
        final Digest.FromString dgst = new Digest.FromString("sha256:1234");
        MatcherAssert.assertThat(dgst.valid(), new IsEqual<>(true));
        MatcherAssert.assertThat("bad algorithm", dgst.alg(), Matchers.is("sha256"));
        MatcherAssert.assertThat("bad digest", dgst.hex(), Matchers.is("1234"));
    }

    @Test
    void failsOnInvalidString() {
        final Digest.FromString dgst = new Digest.FromString("asd");
        MatcherAssert.assertThat(dgst.valid(), new IsEqual<>(false));
        Assertions.assertThrows(
            IllegalStateException.class, () -> dgst.alg(), "alg() didn't fail"
        );
        Assertions.assertThrows(
            IllegalStateException.class, () -> dgst.hex(), "digest() didn't fail"
        );
    }

    @Test
    void shouldHaveExpectedStringRepresentation() {
        final Digest.Sha256 digest = new Digest.Sha256(
            "6c3c624b58dbbcd3c0dd82b4c53f04194d1247c6eebdaab7c610cf7d66709b3b"
        );
        MatcherAssert.assertThat(
            digest.string(),
            new IsEqual<>("sha256:6c3c624b58dbbcd3c0dd82b4c53f04194d1247c6eebdaab7c610cf7d66709b3b")
        );
    }
}
