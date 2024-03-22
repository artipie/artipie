/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.BaseResponse;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Tests for {@link ProxyLayers}.
 */
class ProxyLayersTest {

    @Test
    void shouldGetBlob() {
        final long size = 10L;
        final String digest = "sha256:123";
        final Optional<Blob> blob = new ProxyLayers(
            (line, headers, body) -> {
                if (!line.toString().startsWith(String.format("HEAD /v2/test/blobs/%s ", digest))) {
                    throw new IllegalArgumentException();
                }
                return BaseResponse.ok().header(new ContentLength(String.valueOf(size)));
            },
            new RepoName.Valid("test")
        ).get(new Digest.FromString(digest)).toCompletableFuture().join();
        MatcherAssert.assertThat(blob.isPresent(), new IsEqual<>(true));
        MatcherAssert.assertThat(
            blob.orElseThrow().digest().string(),
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
                if (!line.toString().startsWith(String.format("HEAD /v2/%s/blobs/%s ", repo, digest))) {
                    throw new IllegalArgumentException();
                }
                return BaseResponse.notFound();
            },
            new RepoName.Valid(repo)
        ).get(new Digest.FromString(digest)).toCompletableFuture().join();
        MatcherAssert.assertThat(found.isPresent(), new IsEqual<>(false));
    }
}
