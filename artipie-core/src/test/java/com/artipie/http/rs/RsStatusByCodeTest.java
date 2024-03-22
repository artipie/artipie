/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RsStatus.ByCode}.
 * @since 0.11
 */
class RsStatusByCodeTest {

    @Test
    void findsStatus() {
        MatcherAssert.assertThat(
            RsStatus.byCode(200),
            new IsEqual<>(RsStatus.OK)
        );
    }

    @Test
    void throwsExceptionIfNotFound() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> RsStatus.byCode(555)
        );
    }

}
