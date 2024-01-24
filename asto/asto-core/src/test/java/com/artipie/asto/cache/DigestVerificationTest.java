/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.cache;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.ext.Digests;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.codec.binary.Hex;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link DigestVerification}.
 *
 * @since 0.25
 */
final class DigestVerificationTest {

    @Test
    void validatesCorrectDigest() throws Exception {
        final boolean result = new DigestVerification(
            Digests.MD5,
            Hex.decodeHex("5289df737df57326fcdd22597afb1fac")
        ).validate(
            new Key.From("any"),
            () -> CompletableFuture.supplyAsync(
                () -> Optional.of(new Content.From(new byte[]{1, 2, 3}))
            )
        ).toCompletableFuture().get();
        MatcherAssert.assertThat(result, Matchers.is(true));
    }

    @Test
    void doesntValidatesIncorrectDigest() throws Exception {
        final boolean result = new DigestVerification(
            Digests.MD5, new byte[16]
        ).validate(
            new Key.From("other"),
            () -> CompletableFuture.supplyAsync(
                () -> Optional.of(new Content.From(new byte[]{1, 2, 3}))
            )
        ).toCompletableFuture().get();
        MatcherAssert.assertThat(result, Matchers.is(false));
    }

    @Test
    void doesntValidateAbsentContent() throws Exception {
        MatcherAssert.assertThat(
            new DigestVerification(
                Digests.MD5, new byte[16]
            ).validate(
                new Key.From("something"),
                () -> CompletableFuture.supplyAsync(Optional::empty)
            ).toCompletableFuture().get(),
            Matchers.is(false)
        );
    }
}
