/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import com.artipie.http.ResponseBuilder;
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Tests for {@link ProxyBlob}.
 */
class ProxyBlobTest {

    @Test
    void shouldReadContent() {
        final byte[] data = "data".getBytes();
        final Content content = new ProxyBlob(
            (line, headers, body) -> {
                if (!line.toString().startsWith("GET /v2/test/blobs/sha256:123 ")) {
                    throw new IllegalArgumentException();
                }
                return ResponseBuilder.ok().body(data).completedFuture();
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
    void shouldFinishSendWhenContentIsBad() {
        final Content content = this.badContent();
        Assertions.assertThrows(CompletionException.class, content::asBytes);
    }

    @Test
    void shouldHandleStatus() {
        final byte[] data = "content".getBytes();
        final CompletableFuture<Content> content = new ProxyBlob(
            (line, headers, body) -> ResponseBuilder.internalError(new IllegalArgumentException()).completedFuture(),
            new RepoName.Valid("test-2"),
            new Digest.FromString("sha256:567"),
            data.length
        ).content().toCompletableFuture();
        Assertions.assertThrows(CompletionException.class, content::join);
    }

    private Content badContent() {
        final byte[] data = "1234".getBytes();
        return new ProxyBlob(
            (line, headers, body) -> ResponseBuilder.ok()
                .body(new Content.From(Flowable.error(new IllegalStateException())))
                .completedFuture(),
            new RepoName.Valid("abc"),
            new Digest.FromString("sha256:987"),
            data.length
        ).content().toCompletableFuture().join();
    }
}
