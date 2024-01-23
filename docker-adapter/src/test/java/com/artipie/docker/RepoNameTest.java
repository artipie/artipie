/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker;

import com.artipie.docker.error.InvalidRepoNameException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test case for {@link RepoName}.
 * @since 0.1
 */
final class RepoNameTest {

    @Test
    void acceptsValidRepoName() {
        MatcherAssert.assertThat(
            new RepoName.Valid("ab/c/0/x-y/c_z/v.p/qqqqqqqqqqqqqqqqqqqqqqq").value(),
            Matchers.not(Matchers.blankOrNullString())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "asd/",
        "asd+zxc",
        "-asd",
        "a/.b"
    })
    void cannotBeInvalid(final String name) {
        Assertions.assertThrows(
            InvalidRepoNameException.class,
            () -> new RepoName.Valid(name).value()
        );
    }

    @Test
    void cannotBeGreaterThanMaxLength() {
        Assertions.assertThrows(
            InvalidRepoNameException.class,
            () -> new RepoName.Valid(RepoNameTest.repeatChar('a', 256)).value()
        );
    }

    /**
     * Generates new string with repeated char.
     * @param chr Char to repeat
     * @param count String size
     */
    private static String repeatChar(final char chr, final int count) {
        final StringBuilder str = new StringBuilder(count);
        for (int pos = 0; pos < count; ++pos) {
            str.append(chr);
        }
        return str.toString();
    }
}
