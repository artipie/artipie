/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import com.artipie.docker.http.DigestHeader;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ManifestReference;
import com.artipie.http.Headers;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link ProxyManifests}.
 */
class ProxyManifestsTest {

    @Test
    void shouldGetManifest() {
        final byte[] data = "{ \"schemaVersion\": 2 }".getBytes();
        final String digest = "sha256:123";
        final Optional<Manifest> found = new ProxyManifests(
            (line, headers, body) -> {
                if (!line.toString().startsWith("GET /v2/test/manifests/abc ")) {
                    throw new IllegalArgumentException();
                }
                return new RsFull(
                    RsStatus.OK,
                    new Headers.From(new DigestHeader(new Digest.FromString(digest))),
                    new Content.From(data)
                );
            },
            new RepoName.Valid("test")
        ).get(ManifestReference.from("abc")).toCompletableFuture().join();
        Assertions.assertTrue(found.isPresent());
        final Manifest manifest = found.orElseThrow();
        Assertions.assertEquals(digest, manifest.digest().string());
        final Content content = manifest.content();
        Assertions.assertArrayEquals(data, content.asBytes());
        Assertions.assertEquals(Optional.of((long) data.length), content.size());

    }

    @Test
    void shouldGetEmptyWhenNotFound() {
        final Optional<Manifest> found = new ProxyManifests(
            (line, headers, body) -> {
                if (!line.toString().startsWith("GET /v2/my-test/manifests/latest ")) {
                    throw new IllegalArgumentException();
                }
                return new RsWithStatus(RsStatus.NOT_FOUND);
            },
            new RepoName.Valid("my-test")
        ).get(ManifestReference.from("latest")).toCompletableFuture().join();
        Assertions.assertFalse(found.isPresent());
    }

    @Test
    void shouldSendRequestCatalogFromRemote() {
        final String name = "my-alpine";
        final int limit = 123;
        final AtomicReference<RequestLine> cline = new AtomicReference<>();
        final AtomicReference<Iterable<Map.Entry<String, String>>> cheaders;
        cheaders = new AtomicReference<>();
        final AtomicReference<byte[]> cbody = new AtomicReference<>();
        new ProxyDocker(
            (line, headers, body) -> {
                cline.set(line);
                cheaders.set(headers);
                return new AsyncResponse(
                    new Content.From(body).asBytesFuture().thenApply(
                        bytes -> {
                            cbody.set(bytes);
                            return StandardRs.EMPTY;
                        }
                    )
                );
            }
        ).catalog(Optional.of(new RepoName.Simple(name)), limit).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Sends expected line to remote",
            cline.get().toString(),
            new StringStartsWith(String.format("GET /v2/_catalog?n=%d&last=%s ", limit, name))
        );
        MatcherAssert.assertThat(
            "Sends no headers to remote",
            cheaders.get(),
            new IsEmptyIterable<>()
        );
        Assertions.assertEquals(0, cbody.get().length, "Sends no body to remote");
    }

    @Test
    void shouldReturnCatalogFromRemote() {
        final byte[] bytes = "{\"repositories\":[\"one\",\"two\"]}".getBytes();
        Assertions.assertArrayEquals(
            bytes,
            new ProxyDocker(
                (line, headers, body) -> new RsWithBody(new Content.From(bytes))
            ).catalog(Optional.empty(), Integer.MAX_VALUE).thenCompose(
                catalog -> catalog.json().asBytesFuture()
            ).toCompletableFuture().join()
        );
    }

    @Test
    void shouldFailReturnCatalogWhenRemoteRespondsWithNotOk() {
        final CompletionStage<Catalog> stage = new ProxyDocker(
            (line, headers, body) -> new RsWithStatus(RsStatus.NOT_FOUND)
        ).catalog(Optional.empty(), Integer.MAX_VALUE);
        Assertions.assertThrows(
            Exception.class,
            () -> stage.toCompletableFuture().join()
        );
    }
}
