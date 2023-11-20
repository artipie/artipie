/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.group;

import com.artipie.http.Connection;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Group response.
 * <p>
 * The list of responses which can be send to connection by specified order.
 * </p>
 * @since 0.11
 */
final class GroupResponse implements Response {

    /**
     * Responses.
     */
    private final List<Response> responses;

    /**
     * New group response.
     * @param responses Responses to group
     */
    GroupResponse(final List<Response> responses) {
        this.responses = responses;
    }

    @Override
    public CompletionStage<Void> send(final Connection con) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        final GroupResults results = new GroupResults(this.responses.size(), future);
        for (int pos = 0; pos < this.responses.size(); ++pos) {
            final GroupConnection connection = new GroupConnection(con, pos, results);
            this.responses.get(pos)
                .send(connection)
                .<CompletionStage<Void>>thenApply(CompletableFuture::completedFuture)
                .exceptionally(
                    throwable -> new RsWithStatus(RsStatus.INTERNAL_ERROR).send(connection)
                );
        }
        return future;
    }

    @Override
    public String toString() {
        return String.format(
            "%s: [%s]", this.getClass().getSimpleName(),
            this.responses.stream().map(Object::toString).collect(Collectors.joining(", "))
        );
    }
}
