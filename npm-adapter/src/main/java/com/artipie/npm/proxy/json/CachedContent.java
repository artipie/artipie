/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy.json;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cached package content representation.
 *
 * @since 0.1
 */
public final class CachedContent extends TransformedContent {
    /**
     * Regexp pattern for asset links.
     */
    private static final String REF_PATTERN = "^(.+)/(%s/-/.+)$";

    /**
     * Package name.
     */
    private final String pkg;

    /**
     * Ctor.
     * @param content Package content to be transformed
     * @param pkg Package name
     */
    public CachedContent(final String content, final String pkg) {
        super(content);
        this.pkg = pkg;
    }

    @Override
    String transformRef(final String ref) {
        final Pattern pattern = Pattern.compile(
            String.format(CachedContent.REF_PATTERN, this.pkg)
        );
        final Matcher matcher = pattern.matcher(ref);
        final String newref;
        if (matcher.matches()) {
            newref = String.format("/%s", matcher.group(2));
        } else {
            newref = ref;
        }
        return newref;
    }
}
