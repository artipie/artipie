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

import com.artipie.asto.Key;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link KeyList}.
 *
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class KeyListTest {

    @Test
    void printsSortedKeys() {
        final KeyList target = new KeyList();
        for (final String item : Arrays.asList("aaa/111", "bbb/111", "aaa/222", "ccc", "bbb/222")) {
            target.add(new Key.From(item));
        }
        MatcherAssert.assertThat(
            target.print(new DummyFormat(new StringBuilder())),
            Matchers.stringContainsInOrder(
                Arrays.asList(
                    "aaa/",
                    "aaa/111",
                    "aaa/222",
                    "bbb/",
                    "bbb/111",
                    "bbb/222",
                    "ccc"
                )
            )
        );
    }

    /**
     * Dummy string builder.
     * @since 0.10
     */
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private static final class DummyFormat implements KeyList.KeysFormat<String> {

        /**
         * String accumulator.
         */
        private final StringBuilder builder;

        /**
         * New string format.
         * @param builder String builder
         */
        private DummyFormat(final StringBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void add(final Key item, final boolean parent) {
            this.builder.append(item.string());
            if (parent) {
                this.builder.append('/');
            }
            this.builder.append('\n');
        }

        @Override
        public String result() {
            return this.builder.toString();
        }
    }
}
