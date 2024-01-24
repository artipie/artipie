/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.asto.Content;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import com.artipie.docker.asto.BlobSource;
import com.artipie.docker.asto.TrustedBlobSource;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ReadWriteLayers}.
 *
 * @since 0.5
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ReadWriteLayersTest {

    @Test
    void shouldCallGetWithCorrectRef() {
        final Digest digest = new Digest.FromString("sha256:123");
        final CaptureGetLayers fake = new CaptureGetLayers();
        new ReadWriteLayers(fake, new CapturePutLayers()).get(digest).toCompletableFuture().join();
        MatcherAssert.assertThat(
            fake.digest(),
            new IsEqual<>(digest)
        );
    }

    @Test
    void shouldCallPutPassingCorrectData() {
        final CapturePutLayers fake = new CapturePutLayers();
        final TrustedBlobSource source = new TrustedBlobSource("data".getBytes());
        new ReadWriteLayers(new CaptureGetLayers(), fake).put(source)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            fake.source(),
            new IsEqual<>(source)
        );
    }

    @Test
    void shouldCallMountPassingCorrectData() {
        final Blob original = new FakeBlob();
        final Blob mounted = new FakeBlob();
        final CaptureMountLayers fake = new CaptureMountLayers(mounted);
        final Blob result = new ReadWriteLayers(new CaptureGetLayers(), fake).mount(original)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Original blob is captured",
            fake.capturedBlob(),
            new IsEqual<>(original)
        );
        MatcherAssert.assertThat(
            "Mounted blob is returned",
            result,
            new IsEqual<>(mounted)
        );
    }

    /**
     * Layers implementation that captures get method for checking
     * correctness of parameters. Put method is unsupported.
     *
     * @since 0.5
     */
    private static class CaptureGetLayers implements Layers {
        /**
         * Layer digest.
         */
        private volatile Digest digestcheck;

        @Override
        public CompletionStage<Blob> put(final BlobSource source) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Blob> mount(final Blob blob) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Optional<Blob>> get(final Digest digest) {
            this.digestcheck = digest;
            return CompletableFuture.completedFuture(Optional.empty());
        }

        public Digest digest() {
            return this.digestcheck;
        }
    }

    /**
     * Layers implementation that captures put method for checking
     * correctness of parameters. Get method is unsupported.
     *
     * @since 0.5
     */
    private static class CapturePutLayers implements Layers {
        /**
         * Captured source.
         */
        private volatile BlobSource csource;

        @Override
        public CompletionStage<Blob> put(final BlobSource source) {
            this.csource = source;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Blob> mount(final Blob blob) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Optional<Blob>> get(final Digest digest) {
            throw new UnsupportedOperationException();
        }

        public BlobSource source() {
            return this.csource;
        }
    }

    /**
     * Layers implementation that captures mount method and returns specified blob.
     * Other methods are not supported.
     *
     * @since 0.10
     */
    private static final class CaptureMountLayers implements Layers {

        /**
         * Blob that is returned by mount method.
         */
        private final Blob rblob;

        /**
         * Captured blob.
         */
        private volatile Blob cblob;

        private CaptureMountLayers(final Blob rblob) {
            this.rblob = rblob;
        }

        @Override
        public CompletionStage<Blob> put(final BlobSource source) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Blob> mount(final Blob pblob) {
            this.cblob = pblob;
            return CompletableFuture.completedFuture(this.rblob);
        }

        @Override
        public CompletionStage<Optional<Blob>> get(final Digest digest) {
            throw new UnsupportedOperationException();
        }

        public Blob capturedBlob() {
            return this.cblob;
        }
    }

    /**
     * Blob without any implementation.
     *
     * @since 0.10
     */
    private static final class FakeBlob implements Blob {

        @Override
        public Digest digest() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Long> size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Content> content() {
            throw new UnsupportedOperationException();
        }
    }
}
