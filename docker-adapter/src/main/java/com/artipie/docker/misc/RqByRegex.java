/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.http.rq.RequestLineFrom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request by RegEx pattern.
 * @since 0.3
 */
public final class RqByRegex {

    /**
     * Request line.
     */
    private final String line;

    /**
     * Pattern.
     */
    private final Pattern regex;

    /**
     * Ctor.
     * @param line Request line
     * @param regex Regex
     */
    public RqByRegex(final String line, final Pattern regex) {
        this.line = line;
        this.regex = regex;
    }

    /**
     * Matches request path by RegEx pattern.
     *
     * @return Path matcher.
     */
    public Matcher path() {
        final String path = new RequestLineFrom(this.line).uri().getPath();
        final Matcher matcher = this.regex.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Unexpected path: %s", path));
        }
        return matcher;
    }
}
