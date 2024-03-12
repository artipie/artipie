/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.google.common.base.Splitter;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Tests for {@link TagsPage}.
 */
final class TagsPageTest {

    @ParameterizedTest
    @CsvSource({
        ",,1;2;3;4;5",
        "2,,3;4;5",
        "7,,''",
        ",2,1;2",
        "2,2,3;4"
    })
    void shouldSupportPaging(final String from, final Integer limit, final String result) {
        final String repo = "my-alpine";
        MatcherAssert.assertThat(
            new TagsPage(
                new RepoName.Simple(repo),
                Arrays.asList("3", "1", "2", "4", "5", "4"),
                Optional.ofNullable(from).map(Tag.Valid::new),
                Optional.ofNullable(limit).orElse(Integer.MAX_VALUE)
            ).json().asString(),
            new StringIsJson.Object(
                Matchers.allOf(
                    new JsonHas("name", new JsonValueIs(repo)),
                    new JsonHas(
                        "tags",
                        new JsonContains(
                            StreamSupport.stream(
                                Splitter.on(";").omitEmptyStrings().split(result).spliterator(),
                                false
                            ).map(JsonValueIs::new).collect(Collectors.toList())
                        )
                    )
                )
            )
        );
    }
}
