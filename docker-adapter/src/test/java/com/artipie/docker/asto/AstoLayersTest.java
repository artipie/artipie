/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Tests for {@link AstoLayers}.
 *
 * @since 0.3
 */
final class AstoLayersTest {

    /**
     * Blobs storage.
     */
    private AstoBlobs blobs;

    /**
     * Layers tested.
     */
    private Layers layers;

    @BeforeEach
    void setUp() {
        this.blobs = new AstoBlobs(new InMemoryStorage());
        this.layers = new AstoLayers(this.blobs);
    }

    @Test
    void shouldAddLayer() {
        final byte[] data = "data".getBytes();
        final Digest digest = this.layers.put(new TrustedBlobSource(data))
            .toCompletableFuture().join().digest();
        final Optional<Blob> found = this.blobs.blob(digest).join();
        MatcherAssert.assertThat(found.isPresent(), new IsEqual<>(true));
        MatcherAssert.assertThat(bytes(found.orElseThrow()), new IsEqual<>(data));
    }

    @Test
    void shouldReadExistingLayer() {
        final byte[] data = "content".getBytes();
        final Digest digest = this.blobs.put(new TrustedBlobSource(data))
            .toCompletableFuture().join().digest();
        final Optional<Blob> found = this.layers.get(digest).toCompletableFuture().join();
        MatcherAssert.assertThat(found.isPresent(), new IsEqual<>(true));
        MatcherAssert.assertThat(found.orElseThrow().digest(), new IsEqual<>(digest));
        MatcherAssert.assertThat(bytes(found.get()), new IsEqual<>(data));
    }

    @Test
    void shouldReadAbsentLayer() {
        final Optional<Blob> found = this.layers.get(
            new Digest.Sha256("0123456789012345678901234567890123456789012345678901234567890123")
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(found.isPresent(), new IsEqual<>(false));
    }

    @Test
    void shouldMountBlob() {
        final byte[] data = "hello world".getBytes();
        final Digest digest = new Digest.Sha256(
            "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
        );
        final Blob blob = this.layers.mount(
            new Blob() {
                @Override
                public Digest digest() {
                    return digest;
                }

                @Override
                public CompletionStage<Long> size() {
                    return CompletableFuture.completedFuture((long) data.length);
                }

                @Override
                public CompletionStage<Content> content() {
                    return CompletableFuture.completedFuture(new Content.From(data));
                }
            }
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Mounted blob has expected digest",
            blob.digest(),
            new IsEqual<>(digest)
        );
        MatcherAssert.assertThat(
            "Mounted blob has expected content",
            bytes(blob),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Mounted blob is in storage",
            this.layers.get(digest).toCompletableFuture().join().isPresent(),
            new IsEqual<>(true)
        );
    }

    private static byte[] bytes(final Blob blob) {
        return blob.content().toCompletableFuture().join().asBytes();
    }
}
