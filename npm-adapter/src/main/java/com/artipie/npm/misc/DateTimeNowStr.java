/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.misc;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides current date and time.
 * @since 0.7.6
 */
public final class DateTimeNowStr {

    /**
     * Current time.
     */
    private final String currtime;

    /**
     * Ctor.
     */
    public DateTimeNowStr() {
        this.currtime = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .format(
                ZonedDateTime.ofInstant(
                    Instant.now(),
                    ZoneOffset.UTC
                )
            );
    }

    /**
     * Current date and time.
     * @return Current date and time.
     */
    public String value() {
        return this.currtime;
    }
}
