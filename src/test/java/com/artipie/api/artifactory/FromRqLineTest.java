/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.api.artifactory;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link FromRqLine}.
 * @since 0.10
 */
final class FromRqLineTest {
    @Test
    void shouldReturnEmptyForBadRqLineUser() {
        final FromRqLine user = new FromRqLine(
            "GET /bad/api/security/users HTTP/1.1", FromRqLine.RqPattern.USER
        );
        MatcherAssert.assertThat(
            user.get().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldReturnUsernameFromRqLine() {
        final String username = "john";
        final FromRqLine user = new FromRqLine(
            String.format("GET /api/security/users/%s HTTP/1.1", username),
            FromRqLine.RqPattern.USER
        );
        MatcherAssert.assertThat(
            user.get().get(),
            new IsEqual<>(username)
        );
    }

    @Test
    void shouldReturnEmptyForBadRqLineRepo() {
        final FromRqLine repo = new FromRqLine(
            "GET /bad/api/security/permissions HTTP/1.1", FromRqLine.RqPattern.REPO
        );
        MatcherAssert.assertThat(
            repo.get().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldReturnRepoFromRqLine() {
        final String docker = "docker";
        final FromRqLine repo = new FromRqLine(
            String.format("GET /api/security/permissions/%s HTTP/1.1", docker),
            FromRqLine.RqPattern.REPO
        );
        MatcherAssert.assertThat(
            repo.get().get(),
            new IsEqual<>(docker)
        );
    }
}
