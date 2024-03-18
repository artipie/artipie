/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.RepoName;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link ProxyDocker}.
 */
final class ProxyDockerTest {

    @Test
    void createsProxyRepo() {
        final ProxyDocker docker = new ProxyDocker((line, headers, body) -> StandardRs.EMPTY);
        MatcherAssert.assertThat(
            docker.repo(new RepoName.Simple("test")),
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
            (line, headers, body) -> {
                cline.set(line.toString());
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
            new IsEqual<>(0)
        );
    }

    @Test
    void shouldReturnCatalogFromRemote() {
        final byte[] bytes = "{\"repositories\":[\"one\",\"two\"]}".getBytes();
        MatcherAssert.assertThat(
            new ProxyDocker(
                (line, headers, body) -> new RsWithBody(new Content.From(bytes))
            ).catalog(Optional.empty(), Integer.MAX_VALUE).thenCompose(
                catalog -> catalog.json().asBytesFuture()
            ).toCompletableFuture().join(),
            new IsEqual<>(bytes)
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
