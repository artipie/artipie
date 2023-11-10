/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.RepoName;
import com.google.common.base.Splitter;
import java.io.StringReader;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.json.Json;
import javax.json.JsonReader;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Tests for {@link CatalogPage}.
 *
 * @since 0.9
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
    void shouldSupportPaging(final String from, final Integer limit, final String result) {
        MatcherAssert.assertThat(
            new PublisherAs(
                new CatalogPage(
                    this.names,
                    Optional.ofNullable(from).map(RepoName.Simple::new),
                    Optional.ofNullable(limit).orElse(Integer.MAX_VALUE)
                ).json()
            ).asciiString().thenApply(
                str -> {
                    try (JsonReader reader = Json.createReader(new StringReader(str))) {
                        return reader.readObject();
                    }
                }
            ).toCompletableFuture().join(),
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
