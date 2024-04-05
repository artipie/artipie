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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Tests for {@link AstoLayers}.
 *
 * @since 0.3
 */
final class AstoLayersTest {

    /**
     * Blobs storage.
     */
    private Blobs blobs;

    /**
     * Layers tested.
     */
    private Layers layers;

    @BeforeEach
    void setUp() {
        this.blobs = new Blobs(new InMemoryStorage());
        this.layers = new AstoLayers(this.blobs);
    }

    @Test
    void shouldAddLayer() {
        final byte[] data = "data".getBytes();
        final Digest digest = this.layers.put(new TrustedBlobSource(data)).join().digest();
        final Optional<Blob> found = this.blobs.blob(digest).join();
        MatcherAssert.assertThat(found.isPresent(), Matchers.is(true));
        MatcherAssert.assertThat(bytes(found.orElseThrow()), Matchers.is(data));
    }

    @Test
    void shouldReadExistingLayer() {
        final byte[] data = "content".getBytes();
        final Digest digest = this.blobs.put(new TrustedBlobSource(data)).join().digest();
        final Optional<Blob> found = this.layers.get(digest).join();
        MatcherAssert.assertThat(found.isPresent(), Matchers.is(true));
        MatcherAssert.assertThat(found.orElseThrow().digest(), Matchers.is(digest));
        MatcherAssert.assertThat(bytes(found.get()), Matchers.is(data));
    }

    @Test
    void shouldReadAbsentLayer() {
        final Optional<Blob> found = this.layers.get(
            new Digest.Sha256("0123456789012345678901234567890123456789012345678901234567890123")
        ).join();
        MatcherAssert.assertThat(found.isPresent(), Matchers.is(false));
    }

    @Test
    void shouldMountBlob() {
        final byte[] data = "hello world".getBytes();
        final Digest digest = new Digest.Sha256(
            "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
        );
        this.layers.mount(
            new Blob() {
                @Override
                public Digest digest() {
                    return digest;
                }

                @Override
                public CompletableFuture<Long> size() {
                    return CompletableFuture.completedFuture((long) data.length);
                }

                @Override
                public CompletableFuture<Content> content() {
                    return CompletableFuture.completedFuture(new Content.From(data));
                }
            }
        ).join();
        MatcherAssert.assertThat(
            "Mounted blob is in storage",
            this.layers.get(digest).toCompletableFuture().join().isPresent(),
            Matchers.is(true)
        );
    }

    private static byte[] bytes(final Blob blob) {
        return blob.content().join().asBytes();
    }
}
