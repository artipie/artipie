/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Digest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Tests for {@link AstoBlobs}.
 *
 * @since 0.6
 */
final class AstoBlobsTest {

    @Test
    void shouldNotSaveExistingBlob() {
        final byte[] bytes = new byte[]{0x00, 0x01, 0x02, 0x03};
        final Digest digest = new Digest.Sha256(
            "054edec1d0211f624fed0cbca9d4f9400b0e491c43742af2c5b0abebf0c990d8"
        );
        final FakeStorage storage = new FakeStorage();
        final AstoBlobs blobs = new AstoBlobs(
            storage, new Layout()
        );
        blobs.put(new TrustedBlobSource(new Content.From(bytes), digest))
            .toCompletableFuture().join();
        blobs.put(new TrustedBlobSource(new Content.From(bytes), digest))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(storage.saves, new IsEqual<>(1));
    }

    /**
     * Fake storage that stores everything in memory and counts save operations.
     *
     * @since 0.6
     */
    private static final class FakeStorage implements Storage {

        /**
         * Origin storage.
         */
        private final Storage origin;

        /**
         * Save operations counter.
         */
        private int saves;

        private FakeStorage() {
            this.origin = new InMemoryStorage();
        }

        @Override
        public CompletableFuture<Boolean> exists(final Key key) {
            return this.origin.exists(key);
        }

        @Override
        public CompletableFuture<Collection<Key>> list(final Key key) {
            return this.origin.list(key);
        }

        @Override
        public CompletableFuture<Void> save(final Key key, final Content content) {
            this.saves += 1;
            return this.origin.save(key, content);
        }

        @Override
        public CompletableFuture<Void> move(final Key source, final Key target) {
            return this.origin.move(source, target);
        }

        @Override
        public CompletableFuture<? extends Meta> metadata(final Key key) {
            return this.origin.metadata(key);
        }

        @Override
        public CompletableFuture<Content> value(final Key key) {
            return this.origin.value(key);
        }

        @Override
        public CompletableFuture<Void> delete(final Key key) {
            return this.origin.delete(key);
        }

        @Override
        public <T> CompletionStage<T> exclusively(
            final Key key,
            final Function<Storage, CompletionStage<T>> function
        ) {
            return this.origin.exclusively(key, function);
        }
    }
}
