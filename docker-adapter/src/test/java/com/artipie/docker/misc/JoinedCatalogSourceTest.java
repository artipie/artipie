/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.docker.RepoName;
import com.artipie.docker.fake.FakeCatalogDocker;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests for {@link JoinedCatalogSource}.
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
                new Pagination(new RepoName.Simple("four"), limit)
            ).catalog().thenCompose(
                catalog -> catalog.json().asStringFuture()
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
                Pagination.empty(),
                new FakeCatalogDocker(
                    () -> {
                        throw new IllegalStateException();
                    }
                ),
                new FakeCatalogDocker(() -> new Content.From(json.getBytes()))
            ).catalog().thenCompose(
                catalog -> catalog.json().asStringFuture()
            ).toCompletableFuture().join(),
            new IsEqual<>(json)
        );
    }
}
