/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.jcabi.log.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Slice that logs incoming requests and outgoing responses.
 */
public final class LoggingSlice implements Slice {

    /**
     * Logging level.
     */
    private final Level level;

    /**
     * Delegate slice.
     */
    private final Slice slice;

    /**
     * @param slice Slice.
     */
    public LoggingSlice(final Slice slice) {
        this(Level.FINE, slice);
    }

    /**
     * @param level Logging level.
     * @param slice Slice.
     */
    public LoggingSlice(final Level level, final Slice slice) {
        this.level = level;
        this.slice = slice;
    }

    @Override
    public CompletableFuture<Response> response(
        RequestLine line, Headers headers, Content body
    ) {
        final StringBuilder msg = new StringBuilder(">> ").append(line);
        LoggingSlice.append(msg, headers);
        Logger.log(this.level, this.slice, msg.toString());
        return slice.response(line, headers, body)
            .thenApply(res -> {
                final StringBuilder sb = new StringBuilder("<< ").append(res.status());
                LoggingSlice.append(sb, res.headers());
                Logger.log(LoggingSlice.this.level, LoggingSlice.this.slice, sb.toString());
                return res;
            });
    }

    /**
     * Append headers to {@link StringBuilder}.
     *
     * @param builder Target {@link StringBuilder}.
     * @param headers Headers to be appended.
     */
    private static void append(StringBuilder builder, Headers headers) {
        for (Header header : headers) {
            builder.append('\n').append(header.getKey()).append(": ").append(header.getValue());
        }
    }
}
