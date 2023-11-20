/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs.common;

import com.artipie.asto.Content;
import com.artipie.http.ArtipieHttpException;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import java.nio.charset.StandardCharsets;

/**
 * Response for Artipie HTTP exception.
 * @since 1.0
 */
public final class RsError extends Response.Wrap {

    /**
     * New response for internal error.
     * @param exc The cause of internal error
     */
    public RsError(final Exception exc) {
        this(new ArtipieHttpException(RsStatus.INTERNAL_ERROR, exc));
    }

    /**
     * New response for exception.
     * @param exc Artipie HTTP exception
     */
    public RsError(final ArtipieHttpException exc) {
        super(RsError.rsForException(exc));
    }

    /**
     * Build response object for exception.
     * @param exc HTTP error exception
     * @return Response
     */
    private static Response rsForException(final ArtipieHttpException exc) {
        final Throwable cause = exc.getCause();
        final StringBuilder body = new StringBuilder();
        body.append(exc.getMessage()).append('\n');
        if (cause != null) {
            body.append(cause.getMessage()).append('\n');
            if (cause.getSuppressed() != null) {
                for (final Throwable suppressed : cause.getSuppressed()) {
                    body.append(suppressed.getMessage()).append('\n');
                }
            }
        }
        return new RsWithBody(
            new RsWithStatus(exc.status()),
            new Content.From(body.toString().getBytes(StandardCharsets.UTF_8))
        );
    }
}
