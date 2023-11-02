/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian.misc;

import com.artipie.asto.test.TestResource;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link SizeAndDigest}.
 * @since 0.6
 */
class SizeAndDigestTest {

    @Test
    void calcsSizeAndDigest() {
        MatcherAssert.assertThat(
            new SizeAndDigest().apply(new TestResource("Packages.gz").asInputStream()),
            new IsEqual<>(
                new ImmutablePair<>(
                    // @checkstyle MagicNumberCheck (1 line)
                    2564L, "c1cfc96b4ca50645c57e10b65fcc89fd1b2b79eb495c9fa035613af7ff97dbff"
                )
            )
        );
    }

}
