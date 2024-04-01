/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.group;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.RsStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Standard group {@link Slice} implementation.
 */
public final class GroupSlice implements Slice {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupSlice.class);

    /**
     * Target slices.
     */
    private final List<Slice> targets;

    /**
     * New group slice.
     * @param targets Slices to group
     */
    public GroupSlice(final Slice... targets) {
        this(Arrays.asList(targets));
    }

    /**
     * New group slice.
     * @param targets Slices to group
     */
    public GroupSlice(final List<Slice> targets) {
        this.targets = Collections.unmodifiableList(targets);
    }

    @Override
    public CompletableFuture<Response> response(
        RequestLine line, Headers headers, Content body
    ) {
        for (Slice remote : targets) {
            try {
                Response res = remote.response(line, headers, body).get();
                if (res.status() != RsStatus.NOT_FOUND) {
                    return CompletableFuture.completedFuture(res);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.warn("Can't get response for remote " + remote, e);
            }
        }
        return CompletableFuture.completedFuture(ResponseBuilder.notFound().build());

    }
}
