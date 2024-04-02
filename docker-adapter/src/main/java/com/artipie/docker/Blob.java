/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.asto.Content;

import java.util.concurrent.CompletableFuture;

/**
 * Blob stored in repository.
 *
 * @since 0.2
 */
public interface Blob {

    /**
     * Blob digest.
     *
     * @return Digest.
     */
    Digest digest();

    /**
     * Read blob size.
     *
     * @return Size of blob in bytes.
     */
    CompletableFuture<Long> size();

    /**
     * Read blob content.
     *
     * @return Content.
     */
    CompletableFuture<Content> content();
}
