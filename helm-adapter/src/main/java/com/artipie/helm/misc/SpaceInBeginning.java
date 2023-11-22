/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.misc;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for receiving position of space at the beginning of the line.
 * @since 1.1.1
 */
public final class SpaceInBeginning {
    /**
     * String line.
     */
    private final String line;

    /**
     * Ctor.
     * @param line String line
     */
    public SpaceInBeginning(final String line) {
        this.line = line;
    }

    /**
     * Obtains last position of space from beginning before meeting any letter.
     * @return Last position of space from beginning before meeting any letter.
     */
    public int last() {
        String trimmed = this.line;
        while (!StringUtils.isEmpty(trimmed) && !Character.isLetter(trimmed.charAt(0))) {
            trimmed = trimmed.substring(1);
        }
        return this.line.length() - trimmed.length();
    }
}
