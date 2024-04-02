/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.MetaCommon;
import com.artipie.asto.Storage;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;

import java.util.concurrent.CompletableFuture;

/**
 * Asto implementation of {@link Blob}.
 */
public final class AstoBlob implements Blob {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Blob key.
     */
    private final Key key;

    /**
     * Blob digest.
     */
    private final Digest digest;

    /**
     * @param storage Storage.
     * @param key Blob key.
     * @param digest Blob digest.
     */
    public AstoBlob(Storage storage, Key key, Digest digest) {
        this.storage = storage;
        this.key = key;
        this.digest = digest;
    }

    @Override
    public Digest digest() {
        return this.digest;
    }

    @Override
    public CompletableFuture<Long> size() {
        return this.storage.metadata(this.key)
            .thenApply(meta -> new MetaCommon(meta).size());
    }

    @Override
    public CompletableFuture<Content> content() {
        return this.storage.value(this.key);
    }
}
