/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.StandardRs;
import io.reactivex.Flowable;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FilterSlice}.
 *
 * @since 1.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
                (line, headers, body) -> StandardRs.OK,
                FiltersTestUtil.yaml("filters:")
            )
        );
    }

    @Test
    void shouldAllow() {
        final FilterSlice slice = new FilterSlice(
            (line, headers, body) -> StandardRs.OK,
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
        MatcherAssert.assertThat(
            slice.response(
                FiltersTestUtil.get(FilterSliceTest.PATH),
                Collections.emptySet(),
                Flowable.empty()
            ),
            new IsEqual<>(StandardRs.OK)
        );
    }

    @Test
    void shouldForbidden() {
        final AtomicReference<RsStatus> res = new AtomicReference<>();
        final FilterSlice slice = new FilterSlice(
            (line, headers, body) -> StandardRs.OK,
            FiltersTestUtil.yaml(
                String.join(
                    System.lineSeparator(),
                    "filters:",
                    "  include:",
                    "  exclude:"
                )
            )
        );
        slice
            .response(
                FiltersTestUtil.get(FilterSliceTest.PATH),
                Collections.emptySet(),
                Flowable.empty()
            )
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
