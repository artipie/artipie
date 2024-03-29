/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.asto.Content;
import com.artipie.http.Headers;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.artipie.http.ResponseImpl;
import com.artipie.http.rq.RequestLine;

/**
 * Route path.
 */
public interface RtPath {
    /**
     * Try respond.
     *
     * @param line    Request line
     * @param headers Headers
     * @param body    Body
     * @return Response if passed routing rule
     */
    Optional<CompletableFuture<ResponseImpl>> response(RequestLine line, Headers headers, Content body);
}
