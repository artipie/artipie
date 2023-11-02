/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.RepoName;
import com.artipie.docker.fake.FakeCatalogDocker;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

/**
 * Tests for {@link JoinedCatalogSource}.
 *
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class JoinedCatalogSourceTest {

    @Test
    void joinsCatalogs() {
        final int limit = 3;
        MatcherAssert.assertThat(
            new JoinedCatalogSource(
                Stream.of(
                    "{\"repositories\":[\"one\",\"two\"]}",
                    "{\"repositories\":[\"one\",\"three\",\"four\"]}"
                ).map(
                    json -> new FakeCatalogDocker(() -> new Content.From(json.getBytes()))
                ).collect(Collectors.toList()),
                Optional.of(new RepoName.Simple("four")),
                limit
            ).catalog().thenCompose(
                catalog -> new PublisherAs(catalog.json()).asciiString()
            ).toCompletableFuture().join(),
            new StringIsJson.Object(
                new JsonHas(
                    "repositories",
                    new JsonContains(
                        new JsonValueIs("one"), new JsonValueIs("three"), new JsonValueIs("two")
                    )
                )
            )
        );
    }

    @Test
    void treatsFailingCatalogAsEmpty() {
        final String json = "{\"repositories\":[\"library/busybox\"]}";
        MatcherAssert.assertThat(
            new JoinedCatalogSource(
                Optional.empty(),
                Integer.MAX_VALUE,
                new FakeCatalogDocker(
                    () -> {
                        throw new IllegalStateException();
                    }
                ),
                new FakeCatalogDocker(() -> new Content.From(json.getBytes()))
            ).catalog().thenCompose(
                catalog -> new PublisherAs(catalog.json()).asciiString()
            ).toCompletableFuture().join(),
            new IsEqual<>(json)
        );
    }
}
