/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs.common;

import com.artipie.http.Response;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

/**
 * Response with JSON document.
 * @since 0.16
 */
public final class RsJson extends Response.Wrap {

    /**
     * Response from Json structure.
     * @param json Json structure
     */
    public RsJson(final JsonStructure json) {
        this(() -> json);
    }

    /**
     * Json response from builder.
     * @param builder JSON object builder
     */
    public RsJson(final JsonObjectBuilder builder) {
        this(builder::build);
    }

    /**
     * Json response from builder.
     * @param builder JSON array builder
     */
    public RsJson(final JsonArrayBuilder builder) {
        this(builder::build);
    }

    /**
     * Response from Json supplier.
     * @param json Json supplier
     */
    public RsJson(final Supplier<? extends JsonStructure> json) {
        this(json, StandardCharsets.UTF_8);
    }

    /**
     * JSON response with charset encoding and {@code 200} status.
     * @param json Json supplier
     * @param encoding Charset encoding
     */
    public RsJson(final Supplier<? extends JsonStructure> json, final Charset encoding) {
        this(RsStatus.OK, json, encoding);
    }

    /**
     * JSON response with charset encoding and status code.
     * @param status Response status code
     * @param json Json supplier
     * @param encoding Charset encoding
     */
    public RsJson(final RsStatus status, final Supplier<? extends JsonStructure> json,
        final Charset encoding) {
        this(new RsWithStatus(status), json, encoding);
    }

    /**
     * Wrap response with JSON supplier with charset encoding.
     * @param origin Response
     * @param json Json supplier
     * @param encoding Charset encoding
     */
    public RsJson(final Response origin, final Supplier<? extends JsonStructure> json,
        final Charset encoding) {
        super(
            new RsWithBody(
                new RsWithHeaders(
                    origin,
                    new ContentType(
                        String.format("application/json; charset=%s", encoding.displayName())
                    )
                ),
                json.get().toString().getBytes(encoding)
            )
        );
    }
}
