/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.docker.RepoName;
import com.google.common.base.Splitter;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Tests for {@link CatalogPage}.
 */
final class CatalogPageTest {

    /**
     * Repository names.
     */
    private Collection<RepoName> names;

    @BeforeEach
    void setUp() {
        this.names = Stream.of("3", "1", "2", "4", "5", "4")
            .map(RepoName.Simple::new)
            .collect(Collectors.toList());
    }

    @ParameterizedTest
    @CsvSource({
        ",,1;2;3;4;5",
        "2,,3;4;5",
        "7,,''",
        ",2,1;2",
        "2,2,3;4"
    })
    void shouldSupportPaging(String from, Integer limit, String result) {
        MatcherAssert.assertThat(
            new CatalogPage(
                this.names,
                Pagination.from(from, limit)
            ).json().asJsonObject(),
            new JsonHas(
                "repositories",
                new JsonContains(
                    StreamSupport.stream(
                        Splitter.on(";").omitEmptyStrings().split(result).spliterator(),
                        false
                    ).map(JsonValueIs::new).collect(Collectors.toList())
                )
            )
        );
    }
}
