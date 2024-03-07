/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.docker.error.InvalidTagNameException;

import java.util.regex.Pattern;

public class Validator {

    /**
     * RegEx tag validation pattern.
     */
    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$");

    /**
     * Validates tag name.
     * <p>Validation rules are the following:
     * A tag name must be valid ASCII and may contain lowercase and
     * uppercase letters, digits, underscores, periods and dashes.
     * A tag name may not start with a period or a dash and may contain a maximum of 128 characters.
     * <p>See <a href="https://docs.docker.com/engine/reference/commandline/tag/">docker tag</a>.
     *
     * @param tag Tag name
     * @return Validated tag name
     */
    public static boolean isValidTag(String tag) {
        return PATTERN.matcher(tag).matches();
    }

    public static String validateTag(String tag) {
        if (!isValidTag(tag)) {
            throw new InvalidTagNameException("Invalid tag: '" + tag + "'");
        }
        return tag;
    }
}
