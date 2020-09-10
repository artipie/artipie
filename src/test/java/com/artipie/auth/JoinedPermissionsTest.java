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
package com.artipie.auth;

import com.artipie.http.auth.Permissions;
import com.google.common.collect.Streams;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test case for {@link JoinedPermissions}.
 *
 * @since 0.11
 */
final class JoinedPermissionsTest {

    @ParameterizedTest
    @CsvSource({
        "allow,absent,true",
        "allow,allow,true",
        "allow,deny,true",
        "deny,absent,false",
        "deny,deny,false",
        "absent,absent,true"
    })
    void shouldAllowWhenExpected(final String one, final String two, final boolean allow) {
        MatcherAssert.assertThat(
            new JoinedPermissions(
                Streams.concat(fake(one), fake(two)).collect(Collectors.toList())
            ).allowed("some name", "some action"),
            new IsEqual<>(allow)
        );
    }

    // @checkstyle ReturnCountCheck (2 lines)
    @SuppressWarnings("PMD.OnlyOneReturn")
    private static Stream<Permissions> fake(final String code) {
        switch (code) {
            case "absent":
                return Stream.empty();
            case "allow":
                return Stream.of((name, action) -> true);
            case "deny":
                return Stream.of((name, action) -> false);
            default:
                throw new IllegalArgumentException(String.format("Unsupported code: %s", code));
        }
    }
}
