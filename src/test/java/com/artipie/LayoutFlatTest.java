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
package com.artipie;

import com.artipie.asto.Key;
import com.artipie.repo.PathPattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test case for {@link Layout.Flat}.
 *
 * @since 0.15
 */
final class LayoutFlatTest {

    @Test
    void hasNoDashboard() {
        MatcherAssert.assertThat(
            new Layout.Flat().hasDashboard(),
            new IsEqual<>(false)
        );
    }

    @Test
    void usesFlatPattern() {
        MatcherAssert.assertThat(
            new Layout.Flat().pattern(),
            new IsEqual<>(new PathPattern("flat").pattern())
        );
    }

    @ParameterizedTest
    @CsvSource({"/one,one", "/some/path,some"})
    void getKeyFromPath(final String path, final String key) {
        MatcherAssert.assertThat(
            new Layout.Flat().keyFromPath(path).get(),
            new IsEqual<>(new Key.From(key))
        );
    }

    @Test
    void getEmptyKeyFromEmptyPath() {
        MatcherAssert.assertThat(
            new Layout.Flat().keyFromPath("").isEmpty(),
            new IsEqual<>(true)
        );
    }
}
