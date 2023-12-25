/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.cache;

import com.artipie.asto.Key;
import com.artipie.asto.ext.ContentDigest;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * By digest verification.
 * @since 0.25
 */
public final class DigestVerification implements CacheControl {

    /**
     * Message digest.
     */
    private final Supplier<MessageDigest> digest;

    /**
     * Expected digest.
     */
    private final byte[] expected;

    /**
     * New digest verification.
     * @param digest Message digest has func
     * @param expected Expected digest bytes
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public DigestVerification(final Supplier<MessageDigest> digest, final byte[] expected) {
        this.digest = digest;
        this.expected = expected;
    }

    @Override
    public CompletionStage<Boolean> validate(final Key item, final Remote content) {
        return content.get().thenCompose(
            val -> val.map(pub -> new ContentDigest(pub, this.digest).bytes())
                .orElse(CompletableFuture.completedFuture(new byte[]{}))
        ).thenApply(actual -> Arrays.equals(this.expected, actual));
    }
}
