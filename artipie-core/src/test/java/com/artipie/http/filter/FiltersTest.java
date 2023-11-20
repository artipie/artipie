/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.artipie.http.Headers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.IsTrue;

/**
 * Test for {@link Filters}.
 *
 * @since 1.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class FiltersTest {
    /**
     * Request path.
     */
    private static final String PATH = "/mvnrepo/com/artipie/inner/0.1/inner-0.1.pom";

    @Test
    void emptyFilterLists() {
        final Filters filters = FiltersTest.createFilters(
            String.join(
                System.lineSeparator(),
                "filters:",
                "  include:",
                "  exclude:"
            )
        );
        MatcherAssert.assertThat(
            filters.allowed(FiltersTestUtil.get(FiltersTest.PATH), Headers.EMPTY),
            IsNot.not(new IsTrue())
        );
    }

    @Test
    void allows() {
        final Filters filters = FiltersTest.createFilters(
            String.join(
                System.lineSeparator(),
                "include:",
                "  glob:",
                "    - filter: **/com/acme/**",
                "    - filter: **/com/artipie/**",
                "exclude:",
                "  glob:",
                "    - filter: **/org/log4j/**"
            )
        );
        MatcherAssert.assertThat(
            filters.allowed(FiltersTestUtil.get(FiltersTest.PATH), Headers.EMPTY),
            new IsTrue()
        );
    }

    @Test
    void allowsMixedFilters() {
        final Filters filters = FiltersTest.createFilters(
            String.join(
                System.lineSeparator(),
                "include:",
                "  glob:",
                "    - filter: **/com/acme/**",
                "    - filter: **/com/artipie/**",
                "  regexp:",
                "    - filter: .*/com/github/.*\\.pom",
                "    - filter: .*/pool/main/.*\\.deb",
                "exclude:",
                "  glob:",
                "    - filter: **/org/log4j/**"
            )
        );
        MatcherAssert.assertThat(
            filters.allowed(
                FiltersTestUtil.get("debian/pool/main/c/cron/cron_3.0pl1-137_amd64.deb"),
                Headers.EMPTY
            ),
            new IsTrue()
        );
    }

    @Test
    void forbidden() {
        final Filters filters = FiltersTest.createFilters(
            String.join(
                System.lineSeparator(),
                "include:",
                "  glob:",
                "    - filter: **/*",
                "exclude:",
                "  glob:",
                "    - filter: **/com/artipie/**"
            )
        );
        MatcherAssert.assertThat(
            filters.allowed(FiltersTestUtil.get(FiltersTest.PATH), Headers.EMPTY),
            IsNot.not(new IsTrue())
        );
    }

    @Test
    void forbidMixedFilters() {
        final Filters filters = FiltersTest.createFilters(
            String.join(
                System.lineSeparator(),
                "include:",
                "  glob:",
                "    - filter: **/org/log4j/**",
                "exclude:",
                "  glob:",
                "    - filter: **/com/acme/**",
                "    - filter: **/com/artipie/**",
                "  regexp:",
                "    - filter: .*/com/github/.*\\.pom",
                "    - filter: .*/pool/main/.*\\.deb"
            )
        );
        MatcherAssert.assertThat(
            filters.allowed(
                FiltersTestUtil.get("debian/pool/main/c/cron/cron_3.0pl1-137_amd64.deb"),
                Headers.EMPTY
            ),
            IsNot.not(new IsTrue())
        );
    }

    /**
     * Creates Filters instance from yaml configuration.
     * @param yaml Yaml configuration for filters.
     * @return Filters
     */
    private static Filters createFilters(final String yaml) {
        return new Filters(FiltersTestUtil.yaml(yaml));
    }
}
