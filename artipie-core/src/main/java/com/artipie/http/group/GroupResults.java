/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.group;

import com.artipie.http.Connection;
import com.artipie.http.rs.StandardRs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Group response results aggregator.
 * @implNote This class is not thread safe and should be synchronized
 * @since 0.11
 */
final class GroupResults {

    /**
     * List of results.
     */
    private final List<GroupResult> list;

    /**
     * Completion future.
     */
    private final CompletableFuture<Void> future;

    /**
     * New results aggregator.
     * @param cap Capacity
     * @param future Future to complete when all results are done
     */
    GroupResults(final int cap, final CompletableFuture<Void> future) {
        this(new ArrayList<>(Collections.nCopies(cap, null)), future);
    }

    /**
     * Primary constructor.
     * @param list List of results
     * @param future Future to complete when all results are done
     */
    private GroupResults(final List<GroupResult> list, final CompletableFuture<Void> future) {
        this.list = list;
        this.future = future;
    }

    /**
     * Complete results.
     * <p>
     * This method checks if the response can be completed. If the result was succeed and
     * all previous ordered results were completed and failed, then the whole response will
     * be replied to the {@link Connection}. If any previous results is not completed, then
     * this result will be placed in the list to wait all previous results.
     * </p>
     * @param order Order of result
     * @param result Repayable result
     * @param con Connection to use for replay
     * @return Future
     * @checkstyle ReturnCountCheck (25 lines)
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    public CompletionStage<Void> complete(final int order, final GroupResult result,
        final Connection con) {
        if (this.future.isDone()) {
            result.cancel();
            return CompletableFuture.completedFuture(null);
        }
        if (order >= this.list.size()) {
            throw new IllegalStateException("Wrong order of result");
        }
        this.list.set(order, result);
        for (int pos = 0; pos < this.list.size(); ++pos) {
            final GroupResult target = this.list.get(pos);
            if (target == null) {
                return CompletableFuture.completedFuture(null);
            }
            if (target.success()) {
                return target.replay(con).thenRun(
                    () -> this.list.stream().filter(Objects::nonNull).forEach(GroupResult::cancel)
                ).thenRun(() -> this.future.complete(null));
            }
        }
        return StandardRs.NOT_FOUND.send(con).thenRun(() -> this.future.complete(null));
    }
}
