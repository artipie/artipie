/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.artipie.asto.Content;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Headers;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link FilterSlice}.
 */
public class FilterSliceTest {
    /**
     * Request path.
     */
    private static final String PATH = "/mvnrepo/com/artipie/inner/0.1/inner-0.1.pom";

    @Test
    void trowsExceptionOnEmptyFiltersConfiguration() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new FilterSlice(
                (line, headers, body) -> CompletableFuture.completedFuture(ResponseBuilder.ok().build()),
                FiltersTestUtil.yaml("filters:")
            )
        );
    }

    @Test
    @Disabled("Response should implement 'equals' method")
    void shouldAllow() {
        final FilterSlice slice = new FilterSlice(
            (line, headers, body) -> CompletableFuture.completedFuture(ResponseBuilder.ok().build()),
            FiltersTestUtil.yaml(
                String.join(
                    System.lineSeparator(),
                    "filters:",
                    "  include:",
                    "    glob:",
                    "      - filter: **/*",
                    "  exclude:"
                )
            )
        );
        Assertions.assertEquals(
            ResponseBuilder.ok().build(),
            slice.response(
                FiltersTestUtil.get(FilterSliceTest.PATH),
                Headers.EMPTY,
                Content.EMPTY
            )
        );
    }

    @Test
    void shouldForbidden() {
        final AtomicReference<RsStatus> res = new AtomicReference<>();
        final FilterSlice slice = new FilterSlice(
            (line, headers, body) -> CompletableFuture.completedFuture(ResponseBuilder.ok().build()),
            FiltersTestUtil.yaml(
                String.join(
                    System.lineSeparator(),
                    "filters:",
                    "  include:",
                    "  exclude:"
                )
            )
        );
        slice.response(
                FiltersTestUtil.get(FilterSliceTest.PATH),
                Headers.EMPTY,
                Content.EMPTY
            ).join()
            .send(
                (status, headers, body) -> {
                    res.set(status);
                    return null;
                }
            );
        MatcherAssert.assertThat(
            res.get(),
            new IsEqual<>(RsStatus.FORBIDDEN)
        );
    }
}
