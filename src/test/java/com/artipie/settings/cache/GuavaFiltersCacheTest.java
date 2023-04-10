/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.filter.Filters;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GuavaFiltersCache}.
 * @since 0.28
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class GuavaFiltersCacheTest {
    /**
     * Maven repository name.
     */
    private static final String MAVEN_REPO = "mavenrepo";

    /**
     * File repository name.
     */
    private static final String FILE_REPO = "filerepo";

    @Test
    void getsValueFromCache() {
        final YamlMapping yaml = GuavaFiltersCacheTest.yaml(
            String.join(
                System.lineSeparator(),
                "filters:",
                "  include:",
                "    glob:",
                "      - filter: **/*",
                "  exclude:",
                "    glob:",
                "      - filter: **/artipie/**/*"
            )
        );
        final GuavaFiltersCache cache = new GuavaFiltersCache();
        final Optional<Filters> first = cache.filters(GuavaFiltersCacheTest.MAVEN_REPO, yaml);
        final Optional<Filters> second = cache.filters(GuavaFiltersCacheTest.MAVEN_REPO, yaml);
        MatcherAssert.assertThat(
            "Obtained filters were different",
            first.equals(second),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Filters was not cached",
            cache.size(),
            new IsEqual<>(1L)
        );
    }

    @Test
    void getsEmptyFilterFromCache() {
        final YamlMapping repoyaml = GuavaFiltersCacheTest.yaml(
            String.join(
                System.lineSeparator(),
                "filters:"
            )
        );
        final GuavaFiltersCache cache = new GuavaFiltersCache();
        final Optional<Filters> filters = cache.filters(GuavaFiltersCacheTest.MAVEN_REPO, repoyaml);
        MatcherAssert.assertThat(
            "Obtained filters were not empty",
            filters.isEmpty(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Filters was not cached",
            cache.size(),
            new IsEqual<>(1L)
        );
    }

    @Test
    void getsFilterWithEmptyListsFromCache() {
        final YamlMapping repoyaml = GuavaFiltersCacheTest.yaml(
            String.join(
                System.lineSeparator(),
                "filters:",
                "  include:",
                "  exclude:"
            )
        );
        final GuavaFiltersCache cache = new GuavaFiltersCache();
        final Optional<Filters> first = cache.filters(GuavaFiltersCacheTest.MAVEN_REPO, repoyaml);
        final Optional<Filters> second = cache.filters(GuavaFiltersCacheTest.MAVEN_REPO, repoyaml);
        MatcherAssert.assertThat(
            "Obtained filters were different",
            first.equals(second),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Filters was not cached",
            cache.size(),
            new IsEqual<>(1L)
        );
    }

    @Test
    void getsDifferentFilters() {
        final YamlMapping repoyaml = GuavaFiltersCacheTest.yaml(
            String.join(
                System.lineSeparator(),
                "filters:",
                "  include:",
                "    glob:",
                "      - filter: **/*",
                "  exclude:"
            )
        );
        final GuavaFiltersCache cache = new GuavaFiltersCache();
        cache.filters(GuavaFiltersCacheTest.MAVEN_REPO, repoyaml);
        cache.filters(GuavaFiltersCacheTest.FILE_REPO, repoyaml);
        MatcherAssert.assertThat(
            "Filters was not cached",
            cache.size(),
            new IsEqual<>(2L)
        );
    }

    /**
     * Create yaml mapping from string.
     * @param yaml String containing yaml configuration
     * @return Yaml mapping
     */
    private static YamlMapping yaml(final String yaml) {
        try {
            return Yaml.createYamlInput(yaml).readYamlMapping();
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }
}
