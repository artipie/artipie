/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.docker.Digest;
import com.artipie.docker.error.InvalidDigestException;
import com.artipie.docker.misc.DigestedFlowable;
import java.util.concurrent.CompletionStage;

/**
 * BlobSource which content is checked against digest on saving.
 *
 * @since 0.12
 */
public final class CheckedBlobSource implements BlobSource {

    /**
     * Blob content.
     */
    private final Content content;

    /**
     * Blob digest.
     */
    private final Digest dig;

    /**
     * Ctor.
     *
     * @param content Blob content.
     * @param dig Blob digest.
     */
    public CheckedBlobSource(final Content content, final Digest dig) {
        this.content = content;
        this.dig = dig;
    }

    @Override
    public Digest digest() {
        return this.dig;
    }

    @Override
    public CompletionStage<Void> saveTo(final Storage storage, final Key key) {
        final DigestedFlowable digested = new DigestedFlowable(this.content);
        final Content checked = new Content.From(
            this.content.size(),
            digested.doOnComplete(
                () -> {
                    final String calculated = digested.digest().hex();
                    final String expected = this.dig.hex();
                    if (!expected.equals(calculated)) {
                        throw new InvalidDigestException(
                            String.format("calculated: %s expected: %s", calculated, expected)
                        );
                    }
                }
            )
        );
        return new TrustedBlobSource(checked, this.dig).saveTo(storage, key);
    }
}
