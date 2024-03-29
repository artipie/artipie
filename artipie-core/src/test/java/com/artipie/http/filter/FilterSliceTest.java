/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

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
    void shouldAllow() {
        final FilterSlice slice = new FilterSlice(
            (line, headers, body) -> ResponseBuilder.ok().completedFuture(),
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
            RsStatus.OK,
            slice.response(
                FiltersTestUtil.get(FilterSliceTest.PATH),
                Headers.EMPTY,
                Content.EMPTY
            ).join().status()
        );
    }

    @Test
    void shouldForbidden() {
        ResponseImpl res = new FilterSlice(
            (line, headers, body) -> ResponseBuilder.ok().completedFuture(),
            FiltersTestUtil.yaml(
                String.join(
                    System.lineSeparator(),
                    "filters:",
                    "  include:",
                    "  exclude:"
                )
            )
        ).response(FiltersTestUtil.get(FilterSliceTest.PATH), Headers.EMPTY, Content.EMPTY)
            .join();
        MatcherAssert.assertThat(
            res.status(),
            Matchers.is(RsStatus.FORBIDDEN)
        );
    }
}
