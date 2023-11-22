/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalized python project name from uploading filename.
 * <p>
 * According to
 * <a href="https://www.python.org/dev/peps/pep-0503/#normalized-names">PEP-503</a> only valid
 * characters in a name are the ASCII alphabet, ASCII numbers, ., -, and _. The name should be
 * lowercased with all runs of the characters ., -, or _ replaced with a single - character.
 * <p>
 * Implementations of this interface should return normalized project name
 * in {@link NormalizedProjectName#value()} method.
 *
 * @since 0.6
 */
public interface NormalizedProjectName {

    /**
     * Python project name.
     * @return String value
     */
    String value();

    /**
     * Simple {@link NormalizedProjectName} implementation: normalise given name
     * by replacing ., -, or _ with a single - and making all characters lowecase. Name can contain
     * ASCII alphabet, ASCII numbers, ., -, and _.
     * @since 0.6
     */
    final class Simple implements NormalizedProjectName {

        /**
         * Pattern to verify the name.
         */
        private static final Pattern VERIFY = Pattern.compile("[A-Za-z0-9.\\-_]+");

        /**
         * Name to normalize.
         */
        private final String name;

        /**
         * Ctor.
         * @param name Name to normalize
         */
        public Simple(final String name) {
            this.name = name;
        }

        @Override
        public String value() {
            if (Simple.VERIFY.matcher(this.name).matches()) {
                return this.name.replaceAll("[-_.]+", "-").toLowerCase(Locale.US);
            }
            throw new IllegalArgumentException(
                "Invalid name: python project should match [A-Za-z0-9.-_]+"
            );
        }
    }

}
