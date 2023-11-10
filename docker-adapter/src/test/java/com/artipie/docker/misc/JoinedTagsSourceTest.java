/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.fake.FullTagsManifests;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

/**
 * Tests for {@link JoinedTagsSource}.
 *
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class JoinedTagsSourceTest {

    @Test
    void joinsTags() {
        final int limit = 3;
        final String name = "my-test";
        MatcherAssert.assertThat(
            new JoinedTagsSource(
                new RepoName.Valid(name),
                Stream.of(
                    "{\"tags\":[\"one\",\"two\"]}",
                    "{\"tags\":[\"one\",\"three\",\"four\"]}"
                ).map(
                    json -> new FullTagsManifests(() -> new Content.From(json.getBytes()))
                ).collect(Collectors.toList()),
                Optional.of(new Tag.Valid("four")),
                limit
            ).tags().thenCompose(
                tags -> new PublisherAs(tags.json()).asciiString()
            ).toCompletableFuture().join(),
            new StringIsJson.Object(
                Matchers.allOf(
                    new JsonHas("name", new JsonValueIs(name)),
                    new JsonHas(
                        "tags",
                        new JsonContains(
                            new JsonValueIs("one"), new JsonValueIs("three"), new JsonValueIs("two")
                        )
                    )
                )
            )
        );
    }
}
