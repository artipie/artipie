/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.asto.Content;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.fake.FullTagsManifests;
import com.artipie.docker.manifest.Manifest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Tests for {@link ReadWriteManifests}.
 */
final class ReadWriteManifestsTest {

    @Test
    void shouldCallGetWithCorrectRef() {
        final ManifestReference ref = ManifestReference.from("get");
        final CaptureGetManifests fake = new CaptureGetManifests();
        new ReadWriteManifests(fake, new CapturePutManifests()).get(ref)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            fake.ref(),
            new IsEqual<>(ref)
        );
    }

    @Test
    void shouldCallPutPassingCorrectData() {
        final byte[] data = "data".getBytes();
        final ManifestReference ref = ManifestReference.from("ref");
        final CapturePutManifests fake = new CapturePutManifests();
        new ReadWriteManifests(new CaptureGetManifests(), fake).put(
            ref,
            new Content.From(data)
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "ManifestRef from put method is wrong.",
            fake.ref(),
            new IsEqual<>(ref)
        );
        MatcherAssert.assertThat(
            "Size of content from put method is wrong.",
            fake.content().size().orElseThrow(),
            new IsEqual<>((long) data.length)
        );
    }

    @Test
    void shouldDelegateTags() {
        final Optional<Tag> from = Optional.of(new Tag.Valid("foo"));
        final int limit = 123;
        final Tags tags = () -> new Content.From("{...}".getBytes());
        final FullTagsManifests fake = new FullTagsManifests(tags);
        final Tags result = new ReadWriteManifests(
            fake,
            new CapturePutManifests()
        ).tags(from, limit).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Forwards from",
            fake.capturedFrom(),
            new IsEqual<>(from)
        );
        MatcherAssert.assertThat(
            "Forwards limit",
            fake.capturedLimit(),
            new IsEqual<>(limit)
        );
        MatcherAssert.assertThat(
            "Returns tags",
            result,
            new IsEqual<>(tags)
        );
    }

    /**
     * Manifests implementation that captures get method for checking
     * correctness of parameters. Put method is unsupported.
     *
     * @since 0.5
     */
    private static class CaptureGetManifests implements Manifests {
        /**
         * Manifest reference.
         */
        private volatile ManifestReference refcheck;

        @Override
        public CompletableFuture<Manifest> put(ManifestReference ref, Content content) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Optional<Manifest>> get(ManifestReference ref) {
            this.refcheck = ref;
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletableFuture<Tags> tags(final Optional<Tag> from, final int limit) {
            throw new UnsupportedOperationException();
        }

        public ManifestReference ref() {
            return this.refcheck;
        }
    }

    /**
     * Manifests implementation that captures put method for checking
     * correctness of parameters. Get method is unsupported.
     *
     * @since 0.5
     */
    private static class CapturePutManifests implements Manifests {
        /**
         * Manifest reference.
         */
        private volatile ManifestReference refcheck;

        /**
         * Manifest content.
         */
        private volatile Content contentcheck;

        @Override
        public CompletableFuture<Manifest> put(ManifestReference ref, Content content) {
            this.refcheck = ref;
            this.contentcheck = content;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<Manifest>> get(ManifestReference ref) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Tags> tags(final Optional<Tag> from, final int limit) {
            throw new UnsupportedOperationException();
        }

        public ManifestReference ref() {
            return this.refcheck;
        }

        public Content content() {
            return this.contentcheck;
        }
    }
}
