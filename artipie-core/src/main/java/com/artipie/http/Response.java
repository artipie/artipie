/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import java.util.concurrent.CompletionStage;

/**
 * HTTP response.
 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html">RFC2616</a>
 */
public interface Response {

    /**
     * Send the response.
     *
     * @param connection Connection to send the response to
     * @return Completion stage for sending response to the connection.
     */
    CompletionStage<Void> send(Connection connection);
}
