/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.error.DockerError;
import com.artipie.http.Response;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.rs.RsStatus;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

/**
 * Docker errors response.
 *
 * @since 0.5
 */
final class ErrorsResponse extends Response.Wrap {

    /**
     * Charset used for JSON encoding.
     */
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    /**
     * Ctor.
     *
     * @param status Response status.
     * @param errors Errors.
     */
    protected ErrorsResponse(final RsStatus status, final DockerError... errors) {
        this(status, Arrays.asList(errors));
    }

    /**
     * Ctor.
     *
     * @param status Response status.
     * @param errors Errors.
     */
    protected ErrorsResponse(final RsStatus status, final Collection<DockerError> errors) {
        super(BaseResponse.from(status).jsonBody(json(errors), ErrorsResponse.CHARSET));
    }

    /**
     * Represent error in JSON format.
     *
     * @param errors Errors.
     * @return JSON string.
     */
    private static String json(final Collection<DockerError> errors) {
        final JsonArrayBuilder array = Json.createArrayBuilder();
        for (final DockerError error : errors) {
            final JsonObjectBuilder obj = Json.createObjectBuilder()
                .add("code", error.code())
                .add("message", error.message());
            error.detail().ifPresent(detail -> obj.add("detail", detail));
            array.add(obj);
        }
        return Json.createObjectBuilder().add("errors", array).build().toString();
    }
}
