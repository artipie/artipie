/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Package hash.
 *
 * @since 0.1
 */
public final class Hash {

    /**
     * Bytes to calculate hash code value from.
     */
    private final Publisher<ByteBuffer> value;

    /**
     * Ctor.
     *
     * @param value Bytes to calculate hash code value from.
     */
    public Hash(final Publisher<ByteBuffer> value) {
        this.value = value;
    }

    /**
     * Saves hash to storage as base64 string.
     *
     * @param storage Storage to use for saving.
     * @param identity Package identity.
     * @return Completion of save operation.
     */
    public CompletionStage<Void> save(final Storage storage, final PackageIdentity identity) {
        return
            new ContentDigest(this.value, Digests.SHA512).bytes().thenCompose(
                bytes -> storage.save(
                    identity.hashKey(),
                    new Content.From(Base64.getEncoder().encode(bytes))
                )
        );
    }
}
