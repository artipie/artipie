/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;

import java.util.concurrent.CompletionStage;

/**
 * Standard responses.
 */
public enum StandardRs implements Response {
    /**
     * Empty response.
     */
    EMPTY(con -> con.accept(RsStatus.OK, Headers.EMPTY, Content.EMPTY)),
    /**
     * OK 200 response.
     */
    OK(EMPTY),
    /**
     * Success response without content.
     */
    NO_CONTENT(con -> con.accept(RsStatus.NO_CONTENT, Headers.EMPTY, Content.EMPTY)),
    /**
     * Not found response.
     */
    NOT_FOUND(con -> con.accept(RsStatus.NOT_FOUND, Headers.EMPTY, Content.EMPTY)),
    /**
     * Not found with json.
     */
    JSON_NOT_FOUND(
        con -> con.accept(
            RsStatus.NOT_FOUND,
            Headers.from("Content-Type", "application/json"),
            new Content.From("{\"error\" : \"not found\"}".getBytes())
        )
    );

    private final Response origin;

    StandardRs(final Response origin) {
        this.origin = origin;
    }

    @Override
    public CompletionStage<Void> send(final Connection connection) {
        return this.origin.send(connection);
    }
}
