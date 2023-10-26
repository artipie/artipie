/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian.misc;

import com.artipie.asto.test.TestResource;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GpgClearsign}.
 * @since 0.4
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class GpgClearsignTest {

    @Test
    void signs() {
        final byte[] release = new TestResource("Release").asBytes();
        final String res = new String(
            new GpgClearsign(release)
            .signedContent(new TestResource("secret-keys.gpg").asBytes(), "1q2w3e4r5t6y7u")
        );
        MatcherAssert.assertThat(
            res,
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContains(new String(release)),
                    new StringContains("-----BEGIN PGP SIGNED MESSAGE-----"),
                    new StringContains("Hash: SHA256"),
                    new StringContains("-----BEGIN PGP SIGNATURE-----"),
                    new StringContains("-----END PGP SIGNATURE-----")
                )
            )
        );
    }

    @Test
    void generatesSignature() {
        final String res = new String(
            new GpgClearsign(new TestResource("Release").asBytes())
            .signature(new TestResource("secret-keys.gpg").asBytes(), "1q2w3e4r5t6y7u")
        );
        MatcherAssert.assertThat(
            res,
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContains("-----BEGIN PGP SIGNATURE-----"),
                    new StringContains("-----END PGP SIGNATURE-----"),
                    new IsNot<>(new StringContains("Version"))
                )
            )
        );
    }

}
