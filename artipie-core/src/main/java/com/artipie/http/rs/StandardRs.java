/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

/**
 * Standard responses.
 * @since 0.8
 */
public enum StandardRs implements Response {
    /**
     * Empty response.
     */
    EMPTY(con -> con.accept(RsStatus.OK, Headers.EMPTY, Flowable.empty())),
    /**
     * OK 200 response.
     */
    OK(EMPTY),
    /**
     * Success response without content.
     */
    NO_CONTENT(new RsWithStatus(RsStatus.NO_CONTENT)),
    /**
     * Not found response.
     */
    NOT_FOUND(new RsWithStatus(RsStatus.NOT_FOUND)),
    /**
     * Not found with json.
     */
    JSON_NOT_FOUND(
        new RsWithBody(
            new RsWithHeaders(
                new RsWithStatus(RsStatus.NOT_FOUND),
                new Headers.From("Content-Type", "application/json")
            ),
            ByteBuffer.wrap("{\"error\" : \"not found\"}".getBytes())
        )
    );

    /**
     * Origin response.
     */
    private final Response origin;

    /**
     * Ctor.
     * @param origin Origin response
     */
    StandardRs(final Response origin) {
        this.origin = origin;
    }

    @Override
    public CompletionStage<Void> send(final Connection connection) {
        return this.origin.send(connection);
    }
}
