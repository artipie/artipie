/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import io.reactivex.Flowable;
import org.hamcrest.Description;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Tests for {@link Upload}.
 */
class UploadTest {

    /**
     * Slice being tested.
     */
    private Upload upload;

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.upload = new Upload(this.storage, "test", UUID.randomUUID().toString());
    }

    @Test
    void shouldCreateDataOnStart() {
        this.upload.start().toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.storage.list(this.upload.root()).join().isEmpty(),
            Matchers.is(false)
        );
    }

    @Test
    void shouldSaveStartedDateWhenLoadingIsStarted() {
        final Instant time = LocalDateTime.of(2020, Month.MAY, 19, 12, 58, 11)
            .atZone(ZoneOffset.UTC).toInstant();
        this.upload.start(time).join();
        MatcherAssert.assertThat(
            new String(
                new BlockingStorage(this.storage)
                    .value(new Key.From(this.upload.root(), "started")),
                StandardCharsets.US_ASCII
            ), Matchers.equalTo("2020-05-19T12:58:11Z")
        );
    }

    @Test
    void shouldReturnOffsetWhenAppendedChunk() {
        final byte[] chunk = "sample".getBytes();
        this.upload.start().join();
        final Long offset = this.upload.append(new Content.From(chunk)).join();
        MatcherAssert.assertThat(offset, Matchers.is((long) chunk.length - 1));
    }

    @Test
    void shouldReadAppendedChunk() {
        final byte[] chunk = "chunk".getBytes();
        this.upload.start().join();
        this.upload.append(new Content.From(chunk)).join();
        MatcherAssert.assertThat(
            this.upload,
            new IsUploadWithContent(chunk)
        );
    }

    @Test
    void shouldFailAppendedSecondChunk() {
        this.upload.start().toCompletableFuture().join();
        this.upload.append(new Content.From("one".getBytes()))
            .join();
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                CompletionException.class,
                () -> this.upload.append(new Content.From("two".getBytes()))
                    .join()
            ).getCause(),
            new IsInstanceOf(UnsupportedOperationException.class)
        );
    }

    @Test
    void shouldAppendedSecondChunkIfFirstOneFailed() {
        this.upload.start().join();
        try {
            this.upload.append(new Content.From(1, Flowable.error(new IllegalStateException())))
                .toCompletableFuture()
                .join();
        } catch (final CompletionException ignored) {
        }
        final byte[] chunk = "content".getBytes();
        this.upload.append(new Content.From(chunk)).join();
        MatcherAssert.assertThat(
            this.upload,
            new IsUploadWithContent(chunk)
        );
    }

    @Test
    void shouldRemoveUploadedFiles() throws ExecutionException, InterruptedException {
        this.upload.start().toCompletableFuture().join();
        final byte[] chunk = "some bytes".getBytes();
        this.upload.append(new Content.From(chunk)).get();
        this.upload.putTo(new CapturePutLayers(), new Digest.Sha256(chunk)).get();
        MatcherAssert.assertThat(
            this.storage.list(this.upload.root()).get(),
            new IsEmptyCollection<>()
        );
    }

    /**
     * Matcher for {@link Upload} content.
     */
    private final class IsUploadWithContent extends TypeSafeMatcher<Upload> {

        /**
         * Expected content.
         */
        private final byte[] content;

        private IsUploadWithContent(final byte[] content) {
            this.content = Arrays.copyOf(content, content.length);
        }

        @Override
        public void describeTo(final Description description) {
            new IsEqual<>(this.content).describeTo(description);
        }

        @Override
        public boolean matchesSafely(final Upload upl) {
            final Digest digest = new Digest.Sha256(this.content);
            final CapturePutLayers fake = new CapturePutLayers();
            upl.putTo(fake, digest).toCompletableFuture().join();
            return new IsEqual<>(this.content).matches(fake.content());
        }
    }

    /**
     * Layers implementation that captures put method content.
     *
     * @since 0.12
     */
    private final class CapturePutLayers implements Layers {

        /**
         * Captured put content.
         */
        private volatile byte[] content;

        @Override
        public CompletableFuture<Digest> put(final BlobSource source) {
            final Key key = new Key.From(UUID.randomUUID().toString());
            source.saveTo(UploadTest.this.storage, key).toCompletableFuture().join();
            this.content = UploadTest.this.storage.value(key)
                .thenCompose(Content::asBytesFuture).join();
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

        public byte[] content() {
            return this.content;
        }
    }
}
