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
import java.util.concurrent.CompletionStage;

/**
 * Asto implementation of {@link Blob}.
 *
 * @since 0.2
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
    private final Digest dig;

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param key Blob key.
     * @param digest Blob digest.
     */
    public AstoBlob(final Storage storage, final Key key, final Digest digest) {
        this.storage = storage;
        this.key = key;
        this.dig = digest;
    }

    @Override
    public Digest digest() {
        return this.dig;
    }

    @Override
    public CompletionStage<Long> size() {
        return this.storage.metadata(this.key).thenApply(
            meta -> new MetaCommon(meta).size()
        );
    }

    @Override
    public CompletionStage<Content> content() {
        return this.storage.value(this.key);
    }
}
