/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test for {@link RsStatus}.
 */
final class RsStatusTest {
    @Test
    void information() {
        Assertions.assertTrue(RsStatus.CONTINUE.information());
    }

    @Test
    void success() {
        Assertions.assertTrue(RsStatus.ACCEPTED.success()
        );
    }

    @Test
    void redirection() {
        Assertions.assertTrue(RsStatus.MOVED_TEMPORARILY.redirection());
    }

    @Test
    void clientError() {
        Assertions.assertTrue(RsStatus.BAD_REQUEST.clientError());
    }

    @Test
    void serverError() {
        Assertions.assertTrue(RsStatus.INTERNAL_ERROR.serverError());
    }

    @ParameterizedTest
    @EnumSource(value = RsStatus.class, names = {"FORBIDDEN", "INTERNAL_ERROR"})
    void error(final RsStatus status) {
        Assertions.assertTrue(status.error());
    }

    @ParameterizedTest
    @EnumSource(value = RsStatus.class, names = {"CONTINUE", "OK", "MOVED_TEMPORARILY"})
    void notError(RsStatus status) {
        Assertions.assertFalse(status.error());
    }
}
