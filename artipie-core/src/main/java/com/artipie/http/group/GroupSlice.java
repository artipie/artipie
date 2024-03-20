/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.group;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Standard group {@link Slice} implementation.
 */
public final class GroupSlice implements Slice {

    /**
     * Methods to broadcast to all target slices.
     */
    private static final Set<RqMethod> BROADCAST_METHODS = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                RqMethod.GET, RqMethod.HEAD, RqMethod.OPTIONS, RqMethod.CONNECT, RqMethod.TRACE
            )
        )
    );

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
    public Response response(final RequestLine line, final Headers headers,
                             final Content body) {
        final Response rsp;
        if (GroupSlice.BROADCAST_METHODS.contains(line.method())) {
            rsp = new GroupResponse(
                this.targets.stream()
                    .map(slice -> slice.response(line, headers, body))
                    .collect(Collectors.toList())
            );
        } else {
            rsp = this.targets.get(0).response(line, headers, body);
        }
        return rsp;
    }
}
