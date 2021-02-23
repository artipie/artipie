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
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Repositories layout.
 *
 * @since 0.14
 */
public interface Layout {

    /**
     * Get repository name RegEx pattern.
     *
     * @return Repository name pattern.
     */
    Pattern pattern();

    /**
     * Key from path.
     * @param path Path from request line
     * @return Key from path.
     */
    Optional<Key> keyFromPath(String path);

    /**
     * Check if layout allows dashboard.
     *
     * @return True if dashboard is supported, false - otherwise.
     */
    boolean hasDashboard();

    /**
     * Split path into parts.
     *
     * @param path Path.
     * @return Array of path parts.
     */
    private static String[] splitPath(final String path) {
        return path.replaceAll("^/+", "").split("/");
    }

    /**
     * Flat layout. Consists of one part.
     *
     * @since 0.14
     */
    final class Flat implements Layout {

        @Override
        public Pattern pattern() {
            return new PathPattern("flat").pattern();
        }

        @Override
        public Optional<Key> keyFromPath(final String path) {
            final String[] parts = splitPath(path);
            final Optional<Key> key;
            if (parts.length >= 1 && !parts[0].isBlank()) {
                key = Optional.of(new Key.From(parts[0]));
            } else {
                key = Optional.empty();
            }
            return key;
        }

        @Override
        public boolean hasDashboard() {
            return false;
        }
    }

    /**
     * Org layout.
     *
     * @since 0.14
     */
    final class Org implements Layout {

        @Override
        public Pattern pattern() {
            return new PathPattern("org").pattern();
        }

        @Override
        public Optional<Key> keyFromPath(final String path) {
            final String[] parts = splitPath(path);
            final Optional<Key> key;
            if (parts.length >= 2) {
                key = Optional.of(new Key.From(parts[0], parts[1]));
            } else {
                key = Optional.empty();
            }
            return key;
        }

        @Override
        public boolean hasDashboard() {
            return true;
        }
    }
}
