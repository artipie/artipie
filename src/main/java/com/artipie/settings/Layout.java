/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.artipie.RqPath;
import com.artipie.asto.Key;
import com.artipie.settings.repo.PathPattern;
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
            if (RqPath.CONDA.test(path)) {
                key = Optional.of(new Key.From(parts[2]));
            } else if (parts.length >= 1 && !parts[0].isBlank()) {
                key = Optional.of(new Key.From(parts[0]));
            } else {
                key = Optional.empty();
            }
            return key;
        }

        @Override
        public String toString() {
            return "flat";
        }
    }

    /**
     * Org layout. Consists of two parts: user name and repository name. Normally,
     * request path has the following form:
     * <code>/username/repo-name/other/parts</code>
     * <p>Thus key can be obtained from first two parts on the path:
     * <code>new Key.From(parts[0], parts[1])</code>.
     * In case of anaconda repository, client can send requests of the following form:
     * <code>/t/conda-user-token/username/repo-name/other/parts</code>
     * <p>In this case key can be obtained as third and forth parts of the path:
     * <code>new Key.From(parts[2], parts[3])</code>
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
            if (RqPath.CONDA.test(path)) {
                // @checkstyle MagicNumberCheck (1 line)
                key = Optional.of(new Key.From(parts[2], parts[3]));
            } else if (parts.length >= 2) {
                key = Optional.of(new Key.From(parts[0], parts[1]));
            } else {
                key = Optional.empty();
            }
            return key;
        }

        @Override
        public String toString() {
            return "org";
        }
    }
}
