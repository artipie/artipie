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

    private final Matcher path;

    public RqByRegex(RequestLine line, Pattern regex) {
        String path = line.uri().getPath();
        Matcher matcher = regex.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unexpected path: " + path);
        }
        this.path = matcher;
    }

    /**
     * Matches request path by RegEx pattern.
     *
     * @return Path matcher.
     */
    public Matcher path() {
        return path;
    }
}
