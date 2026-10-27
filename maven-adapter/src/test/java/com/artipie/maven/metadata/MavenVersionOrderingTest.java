/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.metadata;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/** Tests Maven-specific artifact version ordering. */
final class MavenVersionOrderingTest {

    @Test
    void servicePackFollowsBaseRelease() {
        MatcherAssert.assertThat(
            Integer.signum(new Version("1.0.0-sp").compareTo(new Version("1.0.0"))),
            new IsEqual<>(1)
        );
    }
}
