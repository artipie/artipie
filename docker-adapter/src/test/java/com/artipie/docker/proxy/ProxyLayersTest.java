/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.headers.ContentLength;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
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
                return ResponseBuilder.ok()
                    .header(new ContentLength(String.valueOf(size)))
                    .completedFuture();
            },
            "test"
        ).get(new Digest.FromString(digest)).toCompletableFuture().join();
        MatcherAssert.assertThat(blob.isPresent(), new IsEqual<>(true));
        MatcherAssert.assertThat(
            blob.orElseThrow().digest().string(),
            Matchers.is(digest)
        );
        MatcherAssert.assertThat(
            blob.get().size().toCompletableFuture().join(),
            Matchers.is(size)
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
                return ResponseBuilder.notFound().completedFuture();
            },
            repo
        ).get(new Digest.FromString(digest)).toCompletableFuture().join();
        MatcherAssert.assertThat(found.isPresent(), new IsEqual<>(false));
    }
}
