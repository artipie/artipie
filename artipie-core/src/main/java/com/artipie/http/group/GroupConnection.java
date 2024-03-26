/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.group;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.rs.RsStatus;

import java.util.concurrent.CompletionStage;

/**
 * One remote target connection.
 *
 * @since 0.11
 */
final class GroupConnection implements Connection {

    /**
     * Origin connection.
     */
    private final Connection origin;

    /**
     * Target order.
     */
    private final int pos;

    /**
     * Response results.
     */
    private final GroupResults results;

    /**
     * New connection for one target.
     * @param origin Origin connection
     * @param pos Order
     * @param results Results
     */
    GroupConnection(final Connection origin, final int pos, final GroupResults results) {
        this.origin = origin;
        this.pos = pos;
        this.results = results;
    }

    @Override
    public CompletionStage<Void> accept(final RsStatus status, final Headers headers,
                                        final Content body) {
        synchronized (this.results) {
            return this.results.complete(
                this.pos, new GroupResult(status, headers, body), this.origin
            );
        }
    }
}
