/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.docker.RepoName;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link ParsedCatalog}.
 *
 * @since 0.10
 */
class ParsedCatalogTest {

    @Test
    void parsesNames() {
        MatcherAssert.assertThat(
            new ParsedCatalog(
                () -> new Content.From("{\"repositories\":[\"one\",\"two\"]}".getBytes())
            ).repos().toCompletableFuture().join()
                .stream()
                .map(RepoName::value)
                .collect(Collectors.toList()),
            Matchers.contains("one", "two")
        );
    }

    @Test
    void parsesEmptyRepositories() {
        MatcherAssert.assertThat(
            new ParsedCatalog(
                () -> new Content.From("{\"repositories\":[]}".getBytes())
            ).repos().toCompletableFuture().join()
                .stream()
                .map(RepoName::value)
                .collect(Collectors.toList()),
            new IsEmptyCollection<>()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "[]", "123"})
    void failsParsingInvalid(final String json) {
        final ParsedCatalog catalog = new ParsedCatalog(() -> new Content.From(json.getBytes()));
        Assertions.assertThrows(
            Exception.class,
            () -> catalog.repos().toCompletableFuture().join()
        );
    }
}
