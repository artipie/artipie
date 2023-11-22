/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.meta;

import java.util.stream.Stream;

/**
 * Python package info.
 * @since 0.6
 */
public interface PackageInfo {

    /**
     * Package name.
     * @return Name of the project
     */
    String name();

    /**
     * Package version.
     * @return Version of the project
     */
    String version();

    /**
     * A one-line summary of what the package does.
     * @return Summary
     */
    String summary();

    /**
     * Implementation of {@link PackageInfo} that parses python metadata PKG-INFO file to obtain
     * required information. For more details see
     * <a href="https://www.python.org/dev/peps/pep-0314/">PEP-314</a>.
     * @since 0.6
     */
    final class FromMetadata implements PackageInfo {

        /**
         * Input.
         */
        private final String input;

        /**
         * Ctor.
         * @param input Input
         */
        public FromMetadata(final String input) {
            this.input = input;
        }

        @Override
        public String name() {
            return this.read("Name");
        }

        @Override
        public String version() {
            return this.read("Version");
        }

        @Override
        public String summary() {
            return this.read("Summary");
        }

        /**
         * Reads header value by name.
         * @param header Header name
         * @return Header value
         */
        private String read(final String header) {
            final String name = String.format("%s:", header);
            return Stream.of(this.input.split("\n"))
                .filter(line -> line.startsWith(name)).findFirst()
                .map(line ->  line.replace(name, "").trim())
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        String.format("Invalid metadata file, header %s not found", header)
                    )
                );
        }
    }
}
