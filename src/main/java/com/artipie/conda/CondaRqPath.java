/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Anaconda client performs requests of the following formats:
 * <code>/t/ol-4ee312d8-9fe2-44d2-bea9-053325e1ffd5/my-conda/noarch/repodata.json</code>
 * Where second part is user token, third is repository/user name depending on the layout,
 * then follows conda repository architecture (for example: noarch, linux-64, win-64 etc).
 * This {@link Predicate} implementation tests whether path is such conda path.
 * @since 0.23
 */
public final class CondaRqPath implements Predicate<String> {

    /**
     * Anaconda path pattern.
     */
    private static final Pattern PTRN = Pattern.compile("/t/.*repodata\\.json");

    @Override
    public boolean test(final String path) {
        final int length = path.split("/").length;
        // @checkstyle MagicNumberCheck (1 line)
        return CondaRqPath.PTRN.matcher(path).matches() && (length == 6 || length == 7);
    }
}
