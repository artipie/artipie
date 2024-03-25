/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;

/**
 * Absent resource, sends HTTP 404 Not Found response to every request.
 */
public final class Absent implements Resource {

    @Override
    public Response get(final Headers headers) {
        return ResponseBuilder.notFound().build();
    }

    @Override
    public Response put(Headers headers, Content body) {
        return ResponseBuilder.notFound().build();
    }
}
