/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.RsStatus;
import java.util.concurrent.CompletionStage;

/**
 * Standard HTTP 404 response for NPM adapter.
 * @since 0.1
 */
public final class RsNotFound implements Response {
    @Override
    public CompletionStage<Void> send(final Connection connection) {
        return connection.accept(
            RsStatus.NOT_FOUND,
            new Headers.From(
                new Header("Content-Type", "application/json")
            ),
            new Content.From("{\"error\" : \"not found\"}".getBytes())
        );
    }
}
