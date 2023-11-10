/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.helm.misc;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides current date and time.
 * @since 0.3
 */
public final class DateTimeNow {
    /**
     * Current time.
     */
    private final String currtime;

    /**
     * Ctor.
     */
    public DateTimeNow() {
        this.currtime = ZonedDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnZZZZZ"));
    }

    /**
     * Current date and time as string.
     * An example of time: 2016-10-06T16:23:20.499814565-06:00.
     * @return Current date and time.
     */
    public String asString() {
        return this.currtime;
    }
}
