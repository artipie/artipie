/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.fake.FakeCatalogDocker;
import com.artipie.docker.proxy.ProxyDocker;
import com.artipie.http.rs.StandardRs;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

/**
 * Tests for {@link CacheDocker}.
 *
 * @since 0.3
 */
final class CacheDockerTest {

    @Test
    void createsCacheRepo() {
        final CacheDocker docker = new CacheDocker(
            new ProxyDocker((line, headers, body) -> StandardRs.EMPTY),
            new AstoDocker(new InMemoryStorage()), Optional.empty(), "*"
        );
        MatcherAssert.assertThat(
            docker.repo(new RepoName.Simple("test")),
            new IsInstanceOf(CacheRepo.class)
        );
    }

    @Test
    void loadsCatalogsFromOriginAndCache() {
        final int limit = 3;
        MatcherAssert.assertThat(
            new CacheDocker(
                fake("{\"repositories\":[\"one\",\"three\",\"four\"]}"),
                fake("{\"repositories\":[\"one\",\"two\"]}"), Optional.empty(), "*"
            ).catalog(Optional.of(new RepoName.Simple("four")), limit).thenCompose(
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

    private static FakeCatalogDocker fake(final String catalog) {
        return new FakeCatalogDocker(() -> new Content.From(catalog.getBytes()));
    }
}
