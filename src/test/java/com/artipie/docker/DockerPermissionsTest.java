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
package com.artipie.docker;

import com.artipie.http.auth.Authentication;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link DockerPermissions}.
 *
 * @since 0.15
 */
class DockerPermissionsTest {

    @ParameterizedTest
    @CsvSource({
        "repository:my-alpine:pull,read",
        "repository:test/image:push,write",
        "registry:catalog:*,read"
    })
    void shouldTranslatePermissions(final String original, final String translated) {
        final AtomicReference<String> captured = new AtomicReference<>();
        new DockerPermissions(
            (user, action) -> {
                captured.set(action);
                return action.equals(translated);
            }
        ).allowed(new Authentication.User("alice"), original);
        MatcherAssert.assertThat(
            captured.get(),
            new IsEqual<>(translated)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldForwardResult(final boolean result) {
        MatcherAssert.assertThat(
            new DockerPermissions((user, action) -> result).allowed(
                new Authentication.User("bob"), "repository:my-test:pull"
            ),
            new IsEqual<>(result)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "repository:my-alpine:read",
        "whatever",
        "foo:bar:"
    })
    void shouldFailForUnexpectedActions(final String action) {
        final DockerPermissions permissions = new DockerPermissions((user, act) -> true);
        Assertions.assertThrows(
            RuntimeException.class,
            () -> permissions.allowed(new Authentication.User("chuck"), action)
        );
    }
}
