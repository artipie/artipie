/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.repo;

import com.artipie.Settings;
import java.io.IOException;
import java.io.UncheckedIOException;
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
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * New layout pattern from settings.
     * @param settings Settings
     */
    public PathPattern(final Settings settings) {
        this.settings = settings;
    }

    /**
     * Layout pattern from settings.
     * @return Regex pattern
     */
    public Pattern pattern() {
        String name;
        try {
            name = this.settings.layout();
        } catch (final IOException err) {
            throw new UncheckedIOException("Failed to parse settings", err);
        }
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
