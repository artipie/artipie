/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;

import java.util.concurrent.CompletionStage;

public record ResponseImpl(RsStatus status, Headers headers, Content body) implements Response {

    @Override
    public CompletionStage<Void> send(Connection connection) {
        return connection.accept(status, headers, body);
    }

    @Override
    public String toString() {
        return "ResponseImpl{" +
            "status=" + status +
            ", headers=" + headers +
            ", hasBody=" + body.size().map(s -> s > 0).orElse(false) +
            '}';
    }
}
