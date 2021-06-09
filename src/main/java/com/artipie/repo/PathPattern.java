/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.repo;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * Layout pattern.
 * @since 0.8
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class PathPattern {

    /**
     * Patterns.
     */
    private static final Map<String, Pattern> PATTERNS = Collections.unmodifiableMap(
        new MapOf<>(
            new MapEntry<>("flat", Pattern.compile("/(?:[^/.]+)(/.*)?")),
            new MapEntry<>("org", Pattern.compile("/(?:[^/.]+)/(?:[^/.]+)(/.*)?"))
        )
    );

    /**
     * Artipie layout.
     */
    private final String layout;

    /**
     * New layout pattern from settings.
     * @param layout Artipie layout
     */
    public PathPattern(final String layout) {
        this.layout = layout;
    }

    /**
     * Layout pattern from settings.
     * @return Regex pattern
     */
    public Pattern pattern() {
        String name = this.layout;
        if (name == null) {
            name = "flat";
        }
        final Pattern pth = PathPattern.PATTERNS.get(name);
        if (pth == null) {
            throw new IllegalStateException(String.format("Unknown layout name: '%s'", name));
        }
        return pth;
    }
}
