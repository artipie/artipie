/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.google.common.primitives.Ints;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Changelog entry.
 *
 * @since 0.8.3
 */
final class ChangelogEntry {

    /**
     * RegEx pattern of changelog entry string.
     */
    private static final Pattern PATTERN = Pattern.compile(
        "\\* (?<date>\\w+ \\w+ \\d+ \\d+) (?<author>[^-]+) (?<content>-.*)",
        Pattern.DOTALL
    );

    /**
     * Origin string.
     */
    private final String origin;

    /**
     * Ctor.
     *
     * @param origin Origin string.
     */
    ChangelogEntry(final String origin) {
        this.origin = origin;
    }

    /**
     * Read author.
     *
     * @return Author string.
     */
    String author() {
        return this.matcher().group("author");
    }

    /**
     * Read date.
     *
     * @return Date in UNIX time.
     */
    int date() {
        final String str = this.matcher().group("date");
        final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd yyyy", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final Date date;
        try {
            date = format.parse(str);
        } catch (final ParseException ex) {
            throw new IllegalStateException(String.format("Failed to parse date: '%s'", str), ex);
        }
        return Ints.checkedCast(TimeUnit.MILLISECONDS.toSeconds(date.getTime()));
    }

    /**
     * Read content.
     *
     * @return Content string.
     */
    String content() {
        return this.matcher().group("content");
    }

    /**
     * Matches origin string by pattern.
     *
     * @return Matcher.
     */
    private Matcher matcher() {
        final Matcher matcher = PATTERN.matcher(this.origin);
        if (!matcher.matches()) {
            throw new IllegalStateException(String.format("Cannot parse: '%s'", this.origin));
        }
        return matcher;
    }
}
