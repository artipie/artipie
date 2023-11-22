/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import java.util.List;
import org.redline_rpm.header.AbstractHeader;

/**
 * RPM package.
 *
 * @since 0.6
 */
public interface Package {

    /**
     * Package metadata.
     * @since 0.6
     */
    interface Meta {
        /**
         * Read header.
         * @param tag Tag name
         * @return Header
         */
        MetaHeader header(AbstractHeader.Tag tag);

        /**
         * RPM file checksum.
         * @return Checksum
         */
        Checksum checksum();

        /**
         * RPM file size.
         * @return File size
         */
        long size();

        /**
         * RPM location href.
         * @return Location string
         */
        String href();

        /**
         * Heaaders range.
         * @return Begin and end values
         */
        int[] range();
    }

    /**
     * Package metadata header.
     * @since 0.6.3
     */
    interface MetaHeader {
        /**
         * String header.
         * @param def Default value
         * @return Header value
         */
        String asString(String def);

        /**
         * Integer header.
         * @param def Default value
         * @return Integer number
         */
        int asInt(int def);

        /**
         * List of strings header.
         * @return List of values
         */
        List<String> asStrings();

        /**
         * Array of ints header.
         * @return Int array
         */
        int[] asInts();
    }
}
