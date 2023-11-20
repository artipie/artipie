/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.http.Headers;
import com.artipie.http.rq.RequestLineFrom;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.IsTrue;

/**
 * Test for {@link RegexpFilter}.
 *
 * @since 1.2
 */
@SuppressWarnings({"PMD.UseLocaleWithCaseConversions", "PMD.AvoidDuplicateLiterals"})
class RegexpFilterTest {
    /**
     * Request path.
     */
    private static final String PATH = "/mvnrepo/com/artipie/inner/0.1/inner-0.1.pom";

    @Test
    void checkInstanceTypeReturnedByLoader() {
        MatcherAssert.assertThat(
            new FilterFactoryLoader().newObject(
                "regexp",
                Yaml.createYamlMappingBuilder()
                    .add(
                        "filter",
                        ".*"
                    ).build()
            ),
            new IsInstanceOf(RegexpFilter.class)
        );
    }

    @Test
    void anythingMatchesFilter() {
        final Filter filter = new FilterFactoryLoader().newObject(
            "regexp",
            Yaml.createYamlMappingBuilder()
                .add("filter", ".*")
                .build()
        );
        MatcherAssert.assertThat(
            filter.check(
                new RequestLineFrom(FiltersTestUtil.get(RegexpFilterTest.PATH)),
                Headers.EMPTY
            ),
            new IsTrue()
        );
    }

    @Test
    void packagePrefixFilter() {
        final Filter filter = new FilterFactoryLoader().newObject(
            "regexp",
            Yaml.createYamlMappingBuilder()
                .add(
                    "filter",
                    ".*/com/artipie/.*"
                ).build()
        );
        MatcherAssert.assertThat(
            filter.check(
                new RequestLineFrom(FiltersTestUtil.get(RegexpFilterTest.PATH)),
                Headers.EMPTY
            ),
            new IsTrue()
        );
    }

    @Test
    void matchByFileExtensionFilter() {
        final Filter filter = new FilterFactoryLoader().newObject(
            "regexp",
            Yaml.createYamlMappingBuilder()
                .add(
                    "filter",
                    ".*/com/artipie/.*\\.pom"
                ).build()
        );
        MatcherAssert.assertThat(
            filter.check(
                new RequestLineFrom(FiltersTestUtil.get(RegexpFilterTest.PATH)),
                Headers.EMPTY
            ),
            new IsTrue()
        );
        MatcherAssert.assertThat(
            filter.check(
                new RequestLineFrom(
                    FiltersTestUtil.get(RegexpFilterTest.PATH.replace(".pom", ".zip"))
                ),
                Headers.EMPTY
            ),
            IsNot.not(new IsTrue())
        );
    }

    @Test
    void matchByJarExtensionInPackageIgnoreCase() {
        final Filter filter = new FilterFactoryLoader().newObject(
            "regexp",
            Yaml.createYamlMappingBuilder()
                .add(
                    "filter",
                    ".*/com/artipie/.*\\.pom"
                )
                .add(
                    "case_insensitive",
                    "true"
                ).build()
        );
        MatcherAssert.assertThat(
            filter.check(
                new RequestLineFrom(FiltersTestUtil.get(RegexpFilterTest.PATH).toUpperCase()),
                Headers.EMPTY
            ),
            new IsTrue()
        );
        MatcherAssert.assertThat(
            filter.check(
                new RequestLineFrom(
                    FiltersTestUtil.get(RegexpFilterTest.PATH.replace(".pom", ".zip").toUpperCase())
                ),
                Headers.EMPTY
            ),
            IsNot.not(new IsTrue())
        );
    }

    @Test
    void matchByFullUri() {
        final Filter filter = new FilterFactoryLoader().newObject(
            "regexp",
            Yaml.createYamlMappingBuilder()
                .add(
                    "filter",
                    ".*/com/artipie/.*\\.pom\\?([^&]+)&(user=M[^&]+).*"
                )
                .add(
                    "full_uri",
                    "true"
                ).build()
        );
        MatcherAssert.assertThat(
            filter.check(
                new RequestLineFrom(
                    FiltersTestUtil.get(
                        String.format("%s?auth=true&user=Mike#dev", RegexpFilterTest.PATH)
                    )
                ),
                Headers.EMPTY
            ),
            new IsTrue()
        );
    }
}
