/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.misc.Pagination;
import com.artipie.http.Headers;
import com.artipie.http.RsStatus;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
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
            docker.paginationRef.get().last(),
            Matchers.is(from)
        );
        MatcherAssert.assertThat(
            "Parses limit",
            docker.paginationRef.get().limit(),
            Matchers.is(limit)
        );
    }

    /**
     * Docker implementation with specified catalog.
     * Values of parameters `from` and `limit` from last call of `catalog` method are captured.
     */
    private static class FakeDocker implements Docker {

        private final Catalog catalog;

        /**
         * From parameter captured.
         */
        private final AtomicReference<Pagination> paginationRef;

        FakeDocker(Catalog catalog) {
            this.catalog = catalog;
            this.paginationRef = new AtomicReference<>();
        }

        @Override
        public Repo repo(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Catalog> catalog(Pagination pagination) {
            this.paginationRef.set(pagination);
            return CompletableFuture.completedFuture(this.catalog);
        }
    }
}
