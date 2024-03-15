/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.http.rq.RequestLine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request by RegEx pattern.
 */
public final class RqByRegex {

    /**
     * Request line.
     */
    private final RequestLine line;

    /**
     * Pattern.
     */
    private final Pattern regex;

    /**
     * @param line Request line
     * @param regex Regex
     */
    @Deprecated
    public RqByRegex(final String line, final Pattern regex) {
        this(RequestLine.from(line), regex);
    }

    public RqByRegex(final RequestLine line, final Pattern regex) {
        this.line = line;
        this.regex = regex;
    }

    /**
     * Matches request path by RegEx pattern.
     *
     * @return Path matcher.
     */
    public Matcher path() {
        final String path = this.line.uri().getPath();
        final Matcher matcher = this.regex.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Unexpected path: %s", path));
        }
        return matcher;
    }
}
