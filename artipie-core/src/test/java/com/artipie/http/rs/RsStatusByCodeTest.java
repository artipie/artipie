/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
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
            new RsStatus.ByCode("200").find(),
            new IsEqual<>(RsStatus.OK)
        );
    }

    @Test
    void throwsExceptionIfNotFound() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new RsStatus.ByCode("000").find()
        );
    }

}
