/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
     * Org layout. Consists of two parts.
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
