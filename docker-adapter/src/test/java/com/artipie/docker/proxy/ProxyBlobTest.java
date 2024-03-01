/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.common.RsError;
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link ProxyBlob}.
 */
class ProxyBlobTest {

    @Test
    void shouldReadContent() {
        final byte[] data = "data".getBytes();
        final Content content = new ProxyBlob(
            (line, headers, body) -> {
                if (!line.startsWith("GET /v2/test/blobs/sha256:123 ")) {
                    throw new IllegalArgumentException();
                }
                return new RsFull(
                    RsStatus.OK,
                    Headers.EMPTY,
                    new Content.From(data)
                );
            },
            new RepoName.Valid("test"),
            new Digest.FromString("sha256:123"),
            data.length
        ).content().toCompletableFuture().join();
        MatcherAssert.assertThat(content.asBytes(), new IsEqual<>(data));
        MatcherAssert.assertThat(
            content.size(),
            new IsEqual<>(Optional.of((long) data.length))
        );
    }

    @Test
    void shouldReadSize() {
        final long size = 1235L;
        final ProxyBlob blob = new ProxyBlob(
            (line, headers, body) -> {
                throw new UnsupportedOperationException();
            },
            new RepoName.Valid("my/test"),
            new Digest.FromString("sha256:abc"),
            size
        );
        MatcherAssert.assertThat(
            blob.size().toCompletableFuture().join(),
            new IsEqual<>(size)
        );
    }

    @Test
    void shouldNotFinishSendWhenContentReceived() {
        final AtomicReference<CompletionStage<Void>> capture = new AtomicReference<>();
        this.captureConnectionAccept(capture, false);
        Assertions.assertFalse(capture.get().toCompletableFuture().isDone());
    }

    @Test
    void shouldFinishSendWhenContentConsumed() {
        final AtomicReference<CompletionStage<Void>> capture = new AtomicReference<>();
        this.captureConnectionAccept(capture, false).asBytes();
        Assertions.assertTrue(capture.get().toCompletableFuture().isDone());
    }

    @Test
    @SuppressWarnings("PMD.EmptyCatchBlock")
    void shouldFinishSendWhenContentIsBad() {
        final AtomicReference<CompletionStage<Void>> capture = new AtomicReference<>();
        this.captureConnectionAccept(capture, true).asBytes();
        Assertions.assertTrue(capture.get().toCompletableFuture().isDone());
    }

    @Test
    void shouldHandleStatus() {
        final byte[] data = "content".getBytes();
        final CompletableFuture<Content> content = new ProxyBlob(
            (line, headers, body) -> new RsError(new IllegalArgumentException()),
            new RepoName.Valid("test-2"),
            new Digest.FromString("sha256:567"),
            data.length
        ).content().toCompletableFuture();
        Assertions.assertThrows(CompletionException.class, content::join);
    }

    private Content captureConnectionAccept(
        final AtomicReference<CompletionStage<Void>> capture,
        final boolean failure
    ) {
        final byte[] data = "1234".getBytes();
        return new ProxyBlob(
            (line, headers, body) -> connection -> {
                final Content content;
                if (failure) {
                    content = new Content.From(Flowable.error(new IllegalStateException()));
                } else {
                    content = new Content.From(data);
                }
                final CompletionStage<Void> accept = connection.accept(
                    RsStatus.OK,
                    new Headers.From(new ContentLength(String.valueOf(data.length))),
                    content
                );
                capture.set(accept);
                return accept;
            },
            new RepoName.Valid("abc"),
            new Digest.FromString("sha256:987"),
            data.length
        ).content().toCompletableFuture().join();
    }
}
