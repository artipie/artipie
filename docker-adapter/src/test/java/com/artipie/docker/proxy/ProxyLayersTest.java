/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import io.reactivex.Flowable;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProxyLayers}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class ProxyLayersTest {

    @Test
    void shouldGetBlob() {
        final long size = 10L;
        final String digest = "sha256:123";
        final Optional<Blob> blob = new ProxyLayers(
            (line, headers, body) -> {
                if (!line.startsWith(String.format("HEAD /v2/test/blobs/%s ", digest))) {
                    throw new IllegalArgumentException();
                }
                return new RsFull(
                    RsStatus.OK,
                    new Headers.From(new ContentLength(String.valueOf(size))),
                    Flowable.empty()
                );
            },
            new RepoName.Valid("test")
        ).get(new Digest.FromString(digest)).toCompletableFuture().join();
        MatcherAssert.assertThat(blob.isPresent(), new IsEqual<>(true));
        MatcherAssert.assertThat(
            blob.get().digest().string(),
            new IsEqual<>(digest)
        );
        MatcherAssert.assertThat(
            blob.get().size().toCompletableFuture().join(),
            new IsEqual<>(size)
        );
    }

    @Test
    void shouldGetEmptyWhenNotFound() {
        final String digest = "sha256:abc";
        final String repo = "my-test";
        final Optional<Blob> found = new ProxyLayers(
            (line, headers, body) -> {
                if (!line.startsWith(String.format("HEAD /v2/%s/blobs/%s ", repo, digest))) {
                    throw new IllegalArgumentException();
                }
                return new RsWithStatus(RsStatus.NOT_FOUND);
            },
            new RepoName.Valid(repo)
        ).get(new Digest.FromString(digest)).toCompletableFuture().join();
        MatcherAssert.assertThat(found.isPresent(), new IsEqual<>(false));
    }
}
