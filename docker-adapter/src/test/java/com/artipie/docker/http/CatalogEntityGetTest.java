/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link DockerSlice}.
 * Catalog GET endpoint.
 */
class CatalogEntityGetTest {

    @Test
    void shouldReturnCatalog() {
        final byte[] catalog = "{...}".getBytes();
        ResponseAssert.check(
            new DockerSlice(new FakeDocker(() -> new Content.From(catalog)))
                .response(new RequestLine(RqMethod.GET, "/v2/_catalog"), Headers.EMPTY, Content.EMPTY)
                .join(),
            RsStatus.OK,
            catalog,
            new ContentLength(catalog.length),
            ContentType.json()
        );
    }

    @Test
    void shouldSupportPagination() {
        final String from = "foo";
        final int limit = 123;
        final FakeDocker docker = new FakeDocker(() -> Content.EMPTY);
        new DockerSlice(docker).response(
            new RequestLine(
                RqMethod.GET,
                String.format("/v2/_catalog?n=%d&last=%s", limit, from)
            ),
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        MatcherAssert.assertThat(
            "Parses from",
            docker.from.get().map(RepoName::value),
            new IsEqual<>(Optional.of(from))
        );
        MatcherAssert.assertThat(
            "Parses limit",
            docker.limit.get(),
            new IsEqual<>(limit)
        );
    }

    /**
     * Docker implementation with specified catalog.
     * Values of parameters `from` and `limit` from last call of `catalog` method are captured.
     *
     * @since 0.8
     */
    private static class FakeDocker implements Docker {

        /**
         * Catalog.
         */
        private final Catalog ctlg;

        /**
         * From parameter captured.
         */
        private final AtomicReference<Optional<RepoName>> from;

        /**
         * Limit parameter captured.
         */
        private final AtomicInteger limit;

        FakeDocker(final Catalog ctlg) {
            this.ctlg = ctlg;
            this.from = new AtomicReference<>();
            this.limit = new AtomicInteger();
        }

        @Override
        public Repo repo(final RepoName name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Catalog> catalog(final Optional<RepoName> pfrom, final int plimit) {
            this.from.set(pfrom);
            this.limit.set(plimit);
            return CompletableFuture.completedFuture(this.ctlg);
        }
    }
}
