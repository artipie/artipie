/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.docker;

import com.artipie.docker.error.InvalidTagNameException;
import java.util.regex.Pattern;

/**
 * Docker image tag.
 * See <a href="https://docs.docker.com/engine/reference/commandline/tag/">docker tag</a>.
 *
 * @since 0.2
 */
public interface Tag {

    /**
     * Tag string.
     *
     * @return Tag as string.
     */
    String value();

    /**
     * Valid tag name.
     * Validation rules are the following:
     * <p>
     * A tag name must be valid ASCII and may contain
     * lowercase and uppercase letters, digits, underscores, periods and dashes.
     * A tag name may not start with a period or a dash and may contain a maximum of 128 characters.
     * </p>
     *
     * @since 0.1
     */
    final class Valid implements Tag {

        /**
         * RegEx tag validation pattern.
         */
        private static final Pattern PATTERN =
            Pattern.compile("^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$");

        /**
         * Original unvalidated value.
         */
        private final String original;

        /**
         * Ctor.
         *
         * @param original Original unvalidated value.
         */
        public Valid(final String original) {
            this.original = original;
        }

        @Override
        public String value() {
            if (!this.valid()) {
                throw new InvalidTagNameException(
                    String.format("Invalid tag: '%s'", this.original)
                );
            }
            return this.original;
        }

        /**
         * Validates digest string.
         *
         * @return True if string is valid digest, false otherwise.
         */
        public boolean valid() {
            return Valid.PATTERN.matcher(this.original).matches();
        }
    }
}
