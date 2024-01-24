/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.npm.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.json.JsonObject;

/**
 * DescSortedVersions.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.OnlyOneReturn")
public final class DescSortedVersions {
    /**
     * Versions.
     */
    private final JsonObject versions;

    /**
     * Ctor.
     *
     * @param versions Versions in json
     */
    public DescSortedVersions(final JsonObject versions) {
        this.versions = versions;
    }

    /**
     * Get desc sorted versions.
     *
     * @return Sorted versions
     */
    public List<String> value() {
        return new ArrayList<>(
            this.versions.keySet()
        ).stream()
            .sorted((v1, v2) -> -1 * compareVersions(v1, v2))
            .collect(Collectors.toList());
    }

    /**
     * Compares two versions.
     *
     * @param v1 Version 1
     * @param v2 Version 2
     * @return Value {@code 0} if {@code v1 == v2};
     *  a value less than {@code 0} if {@code v1 < v2}; and
     *  a value greater than {@code 0} if {@code v1 > v2}
     */
    private static int compareVersions(final String v1, final String v2) {
        final String delimiter = "\\.";
        final String[] component1 = v1.split(delimiter);
        final String[] component2 = v2.split(delimiter);
        final int length = Math.min(component1.length, component2.length);
        int result;
        for (int index = 0; index < length; index++) {
            result = Integer.valueOf(component1[index])
                .compareTo(Integer.parseInt(component2[index]));
            if (result != 0) {
                return result;
            }
        }
        result = Integer.compare(component1.length, component2.length);
        return result;
    }
}
