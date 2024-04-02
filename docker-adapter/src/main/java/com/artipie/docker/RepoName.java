/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker;

import com.artipie.docker.error.InvalidRepoNameException;
import java.util.regex.Pattern;

/**
 * Docker image repository name.
 */
public interface RepoName {

    /**
     * Name string.
     * @return Name as string
     */
    String value();

    /**
     * Valid repo name.
     * <p>
     * Classically, repository names have always been two path components
     * where each path component is less than 30 characters.
     * The V2 registry API does not enforce this.
     * The rules for a repository name are as follows:
     * <ul>
     * <li>A repository name is broken up into path components</li>
     * <li>A component of a repository name must be at least one lowercase,
     * alpha-numeric characters, optionally separated by periods,
     * dashes or underscores.More strictly,
     * it must match the regular expression:
     * {@code [a-z0-9]+(?:[._-][a-z0-9]+)*}</li>
     * <li>If a repository name has two or more path components,
     * they must be separated by a forward slash {@code /}</li>
     * <li>The total length of a repository name, including slashes,
     * must be less than 256 characters</li>
     * </ul>
     * </p>
     * @since 0.1
     */
    final class Valid implements RepoName {

        /**
         * Repository name part pattern.
         */
        private static final Pattern PART_PTN =
            Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*");

        /**
         * Repository name max length.
         */
        private static final int MAX_NAME_LEN = 256;

        /**
         * Source string.
         */
        private final RepoName origin;

        /**
         * Ctor.
         * @param name Repo name string
         */
        public Valid(final String name) {
            this(new Simple(name));
        }

        /**
         * Ctor.
         * @param origin Origin repo name
         */
        public Valid(final RepoName origin) {
            this.origin = origin;
        }

        @Override
        @SuppressWarnings("PMD.CyclomaticComplexity")
        public String value() {
            final String src = this.origin.value();
            final int len = src.length();
            if (len < 1 || len >= Valid.MAX_NAME_LEN) {
                throw new InvalidRepoNameException(
                    String.format(
                        "repo name must be between 1 and %d chars long",
                        Valid.MAX_NAME_LEN
                    )
                );
            }
            if (src.charAt(len - 1) == '/') {
                throw new InvalidRepoNameException(
                    "repo name can't end with a slash"
                );
            }
            final String[] parts = src.split("/");
            if (parts.length == 0) {
                throw new InvalidRepoNameException("repo name can't be empty");
            }
            for (final String part : parts) {
                if (!Valid.PART_PTN.matcher(part).matches()) {
                    throw new InvalidRepoNameException(
                        String.format("invalid repo name part: %s", part)
                    );
                }
            }
            return src;
        }
    }

    /**
     * Simple repo name. Can be used for tests as fake object.
     * @since 0.1
     */
    final class Simple implements RepoName {

        /**
         * Repository name string.
         */
        private final String name;

        /**
         * Ctor.
         * @param name Repo name string
         */
        public Simple(final String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return this.name;
        }
    }
}
