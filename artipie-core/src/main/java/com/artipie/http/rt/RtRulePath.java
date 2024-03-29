/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Rule-based route path.
 * <p>
 * A path to slice with routing rule. If
 * {@link RtRule} passed, then the request will be redirected to
 * underlying {@link Slice}.
 */
public final class RtRulePath implements RtPath {

    /**
     * Routing rule.
     */
    private final RtRule rule;

    /**
     * Slice under route.
     */
    private final Slice slice;

    /**
     * New routing path.
     * @param rule Rules to apply
     * @param slice Slice to call
     */
    public RtRulePath(final RtRule rule, final Slice slice) {
        this.rule = rule;
        this.slice = slice;
    }

    @Override
    public Optional<CompletableFuture<ResponseImpl>> response(RequestLine line, Headers headers, Content body) {
        if (this.rule.apply(line, headers)) {
            return Optional.of(this.slice.response(line, headers, body));
        }
        return Optional.empty();
    }
}
