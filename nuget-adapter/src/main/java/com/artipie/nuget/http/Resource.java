/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;

/**
 * Resource serving HTTP requests.
 */
public interface Resource {
    /**
     * Serve GET method.
     *
     * @param headers Request headers.
     * @return Response to request.
     */
    Response get(Headers headers);

    /**
     * Serve PUT method.
     *
     * @param headers Request headers.
     * @param body Request body.
     * @return Response to request.
     */
    Response put(Headers headers, Content body);
}
