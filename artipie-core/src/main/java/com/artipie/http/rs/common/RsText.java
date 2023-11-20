/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs.common;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Response with text.
 * @since 0.16
 */
public final class RsText extends Response.Wrap {

    /**
     * New text response with {@link CharSequence} and {@code UT8} encoding.
     * @param text Char sequence
     */
    public RsText(final CharSequence text) {
        this(text, StandardCharsets.UTF_8);
    }

    /**
     * New text response with {@link CharSequence} and encoding {@link Charset}.
     * @param text Char sequence
     * @param encoding Charset
     */
    public RsText(final CharSequence text, final Charset encoding) {
        this(RsStatus.OK, text, encoding);
    }

    /**
     * New text response with {@link CharSequence} and encoding {@link Charset}.
     * @param status Response status
     * @param text Char sequence
     * @param encoding Charset
     */
    public RsText(final RsStatus status, final CharSequence text, final Charset encoding) {
        this(new RsWithStatus(status), text, encoding);
    }

    /**
     * Wrap existing response with text of {@link CharSequence} and encoding {@link Charset}.
     * @param origin Response
     * @param text Char sequence
     * @param encoding Charset
     */
    public RsText(final Response origin, final CharSequence text, final Charset encoding) {
        super(
            new RsWithBody(
                new RsWithHeaders(
                    origin,
                    new Headers.From(
                        new ContentType(
                            String.format("text/plain; charset=%s", encoding.displayName())
                        )
                    )
                ),
                text, encoding
            )
        );
    }
}
