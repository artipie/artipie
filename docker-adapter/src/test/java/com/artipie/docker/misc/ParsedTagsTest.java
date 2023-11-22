/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.docker.Tag;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link ParsedTags}.
 *
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ParsedTagsTest {

    @Test
    void parsesTags() {
        MatcherAssert.assertThat(
            new ParsedTags(
                () -> new Content.From("{\"tags\":[\"one\",\"two\"]}".getBytes())
            ).tags().toCompletableFuture().join()
                .stream()
                .map(Tag::value)
                .collect(Collectors.toList()),
            Matchers.contains("one", "two")
        );
    }

    @Test
    void parsesName() {
        MatcherAssert.assertThat(
            new ParsedTags(
                () -> new Content.From("{\"name\":\"foo\"}".getBytes())
            ).repo().toCompletableFuture().join().value(),
            new IsEqual<>("foo")
        );
    }

    @Test
    void parsesEmptyTags() {
        MatcherAssert.assertThat(
            new ParsedTags(
                () -> new Content.From("{\"tags\":[]}".getBytes())
            ).tags().toCompletableFuture().join(),
            new IsEmptyCollection<>()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "[]", "123", "{\"name\":\"foo\"}"})
    void failsParsingTagsFromInvalid(final String json) {
        final ParsedTags catalog = new ParsedTags(() -> new Content.From(json.getBytes()));
        Assertions.assertThrows(
            Exception.class,
            () -> catalog.tags().toCompletableFuture().join()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "[]", "123", "{\"tags\":[]}"})
    void failsParsingRepoFromInvalid(final String json) {
        final ParsedTags catalog = new ParsedTags(() -> new Content.From(json.getBytes()));
        Assertions.assertThrows(
            Exception.class,
            () -> catalog.repo().toCompletableFuture().join()
        );
    }
}
