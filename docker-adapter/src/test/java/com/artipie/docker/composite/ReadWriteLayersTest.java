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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
        new ReadWriteLayers(new CaptureGetLayers(), fake)
            .mount(original).join();
        MatcherAssert.assertThat(
            "Original blob is captured",
            fake.capturedBlob(),
            Matchers.is(original)
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
        public CompletableFuture<Digest> put(final BlobSource source) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Void> mount(final Blob blob) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Optional<Blob>> get(final Digest digest) {
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
        private volatile BlobSource source;

        @Override
        public CompletableFuture<Digest> put(final BlobSource source) {
            this.source = source;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> mount(final Blob blob) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Optional<Blob>> get(final Digest digest) {
            throw new UnsupportedOperationException();
        }

        public BlobSource source() {
            return this.source;
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
        public CompletableFuture<Digest> put(final BlobSource source) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Void> mount(final Blob pblob) {
            this.cblob = pblob;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<Blob>> get(final Digest digest) {
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
        public CompletableFuture<Long> size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Content> content() {
            throw new UnsupportedOperationException();
        }
    }
}
