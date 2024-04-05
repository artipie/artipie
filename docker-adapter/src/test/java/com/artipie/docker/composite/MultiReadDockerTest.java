/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.fake.FakeCatalogDocker;
import com.artipie.docker.misc.Pagination;
import com.artipie.docker.proxy.ProxyDocker;
import com.artipie.http.ResponseBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

import java.util.Arrays;

/**
 * Tests for {@link MultiReadDocker}.
 */
final class MultiReadDockerTest {

    @Test
    void createsMultiReadRepo() {
        final MultiReadDocker docker = new MultiReadDocker(
            Arrays.asList(
                new ProxyDocker("registry", (line, headers, body) -> ResponseBuilder.ok().completedFuture()),
                new AstoDocker("registry", new InMemoryStorage())
            )
        );
        MatcherAssert.assertThat(
            docker.repo("test"),
            new IsInstanceOf(MultiReadRepo.class)
        );
    }

    @Test
    void joinsCatalogs() {
        final int limit = 3;
        MatcherAssert.assertThat(
            new MultiReadDocker(
                new FakeCatalogDocker(() -> new Content.From("{\"repositories\":[\"one\",\"two\"]}".getBytes())),
                new FakeCatalogDocker(() -> new Content.From("{\"repositories\":[\"one\",\"three\",\"four\"]}".getBytes()))
            ).catalog(Pagination.from("four", limit))
                .thenCompose(catalog -> catalog.json().asStringFuture())
                .join(),
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
}
