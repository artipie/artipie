/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.docker.Digest;

import java.util.concurrent.CompletableFuture;

/**
 * BlobSource which content is trusted and does not require digest validation.
 */
public final class TrustedBlobSource implements BlobSource {

    /**
     * Blob digest.
     */
    private final Digest dig;

    /**
     * Blob content.
     */
    private final Content content;

    /**
     * @param bytes Blob bytes.
     */
    public TrustedBlobSource(final byte[] bytes) {
        this(new Content.From(bytes), new Digest.Sha256(bytes));
    }

    /**
     * @param content Blob content.
     * @param dig Blob digest.
     */
    public TrustedBlobSource(final Content content, final Digest dig) {
        this.dig = dig;
        this.content = content;
    }

    @Override
    public Digest digest() {
        return this.dig;
    }

    @Override
    public CompletableFuture<Void> saveTo(Storage storage, Key key) {
        return storage.exists(key)
            .thenCompose(
                exists -> exists ? CompletableFuture.completedFuture(null)
                    : storage.save(key, content)
        );
    }
}
