/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
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
 * Tests for {@link AstoTags}.
 */
final class AstoTagsTest {

    /**
     * Repository name used in tests.
     */
    private RepoName name;

    /**
     * Tag keys.
     */
    private Collection<Key> keys;

    @BeforeEach
    void setUp() {
        this.name = new RepoName.Simple("test");
        this.keys = Stream.of("foo/1.0", "foo/0.1-rc", "foo/latest", "foo/0.1")
            .map(Key.From::new)
            .collect(Collectors.toList());
    }

    @ParameterizedTest
    @CsvSource({
        ",,0.1;0.1-rc;1.0;latest",
        "0.1-rc,,1.0;latest",
        "xyz,,''",
        ",2,0.1;0.1-rc",
        "0.1,2,0.1-rc;1.0"
    })
    void shouldSupportPaging(final String from, final Integer limit, final String result) {
        MatcherAssert.assertThat(
            new AstoTags(
                this.name,
                new Key.From("foo"),
                this.keys,
                Optional.ofNullable(from).map(Tag.Valid::new),
                Optional.ofNullable(limit).orElse(Integer.MAX_VALUE)
            ).json().asJsonObject(),
            new JsonHas(
                "tags",
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
