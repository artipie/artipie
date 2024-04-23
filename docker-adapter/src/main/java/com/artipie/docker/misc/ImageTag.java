/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.docker.error.InvalidTagNameException;

import java.util.regex.Pattern;

public class ImageTag {

    /**
     * RegEx tag validation pattern.
     */
    private static final Pattern PATTERN =
        Pattern.compile("^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$");

    /**
     * Valid tag name.
     * Validation rules are the following:
     * <p>
     * A tag name must be valid ASCII and may contain
     * lowercase and uppercase letters, digits, underscores, periods and dashes.
     * A tag name may not start with a period or a dash and may contain a maximum of 128 characters.
     */
    public static String validate(String tag) {
        if (!valid(tag)) {
            throw new InvalidTagNameException(
                String.format("Invalid tag: '%s'", tag)
            );
        }
        return tag;
    }

    public static boolean valid(String tag) {
        return PATTERN.matcher(tag).matches();
    }
}
