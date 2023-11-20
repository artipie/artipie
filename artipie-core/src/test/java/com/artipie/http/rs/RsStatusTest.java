/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test for {@link RsStatus}.
 *
 * @since 0.16
 */
final class RsStatusTest {
    @Test
    void information() {
        final RsStatus status = RsStatus.CONTINUE;
        MatcherAssert.assertThat(
            status.information(),
            new IsEqual<>(true)
        );
    }

    @Test
    void success() {
        final RsStatus status = RsStatus.ACCEPTED;
        MatcherAssert.assertThat(
            status.success(),
            new IsEqual<>(true)
        );
    }

    @Test
    void redirection() {
        final RsStatus status = RsStatus.FOUND;
        MatcherAssert.assertThat(
            status.redirection(),
            new IsEqual<>(true)
        );
    }

    @Test
    void clientError() {
        final RsStatus status = RsStatus.BAD_REQUEST;
        MatcherAssert.assertThat(
            status.clientError(),
            new IsEqual<>(true)
        );
    }

    @Test
    void serverError() {
        final RsStatus status = RsStatus.INTERNAL_ERROR;
        MatcherAssert.assertThat(
            status.serverError(),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @EnumSource(value = RsStatus.class, names = {"FORBIDDEN", "INTERNAL_ERROR"})
    void error(final RsStatus status) {
        MatcherAssert.assertThat(
            status.error(),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @EnumSource(value = RsStatus.class, names = {"CONTINUE", "OK", "FOUND"})
    void notError(final RsStatus status) {
        MatcherAssert.assertThat(
            status.error(),
            new IsEqual<>(false)
        );
    }
}
