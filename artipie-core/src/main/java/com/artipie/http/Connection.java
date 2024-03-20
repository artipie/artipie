/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.http.rs.RsStatus;

import java.util.concurrent.CompletionStage;

/**
 * The http connection.
 */
public interface Connection {

    /**
     * Respond on connection.
     * @param status The http status code.
     * @param headers The http response headers.
     * @param body The http response body.
     * @return Completion stage for accepting HTTP response.
     */
    CompletionStage<Void> accept(RsStatus status, Headers headers, Content body);
}
