/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.misc.Pagination;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.headers.Header;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link ProxyDocker}.
 */
final class ProxyDockerTest {

    @Test
    void createsProxyRepo() {
        final ProxyDocker docker = new ProxyDocker("test_registry", (line, headers, body) ->
            ResponseBuilder.ok().completedFuture());
        MatcherAssert.assertThat(
            docker.repo("test"),
            new IsInstanceOf(ProxyRepo.class)
        );
    }

    @Test
    void shouldSendRequestCatalogFromRemote() {
        final String name = "my-alpine";
        final int limit = 123;
        final AtomicReference<String> cline = new AtomicReference<>();
        final AtomicReference<Iterable<Header>> cheaders;
        cheaders = new AtomicReference<>();
        final AtomicReference<byte[]> cbody = new AtomicReference<>();
        new ProxyDocker(
            "test_registry",
            (line, headers, body) -> {
                cline.set(line.toString());
                cheaders.set(headers);
                return new Content.From(body).asBytesFuture().thenApply(
                    bytes -> {
                        cbody.set(bytes);
                        return ResponseBuilder.ok().build();
                    }
                );
            }
        ).catalog(Pagination.from(name, limit)).join();
        MatcherAssert.assertThat(
            "Sends expected line to remote",
            cline.get(),
            new StringStartsWith(String.format("GET /v2/_catalog?n=%d&last=%s ", limit, name))
        );
        MatcherAssert.assertThat(
            "Sends no headers to remote",
            cheaders.get(),
            new IsEmptyIterable<>()
        );
        MatcherAssert.assertThat(
            "Sends no body to remote",
            cbody.get().length,
            Matchers.is(0)
        );
    }

    @Test
    void shouldReturnCatalogFromRemote() {
        final byte[] bytes = "{\"repositories\":[\"one\",\"two\"]}".getBytes();
        MatcherAssert.assertThat(
            new ProxyDocker(
                "test_registry",
                (line, headers, body) -> ResponseBuilder.ok().body(bytes).completedFuture()
            ).catalog(Pagination.empty()).thenCompose(
                catalog -> catalog.json().asBytesFuture()
            ).join(),
            new IsEqual<>(bytes)
        );
    }

    @Test
    void shouldFailReturnCatalogWhenRemoteRespondsWithNotOk() {
        final CompletionStage<Catalog> stage = new ProxyDocker(
            "test_registry",
            (line, headers, body) -> ResponseBuilder.notFound().completedFuture()
        ).catalog(Pagination.empty());
        Assertions.assertThrows(
            Exception.class,
            () -> stage.toCompletableFuture().join()
        );
    }
}
