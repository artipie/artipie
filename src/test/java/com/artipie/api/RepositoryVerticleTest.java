/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.nuget.RandomFreePort;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for {@link RepositoryVerticle}.
 * @since 0.26
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RepositoryVerticleTest {

    /**
     * Test storage.
     */
    private static final BlockingStorage ASTO = new BlockingStorage(new InMemoryStorage());

    /**
     * Service port.
     */
    private static int port;

    @BeforeAll
    static void prepare(final Vertx vertx, final VertxTestContext ctx) throws IOException {
        RepositoryVerticleTest.port = new RandomFreePort().value();
        RepositoryVerticleTest.ASTO.save(new Key.From("artipie/docker-repo.yaml"), new byte[]{});
        RepositoryVerticleTest.ASTO.save(new Key.From("alice/rpm-local.yml"), new byte[]{});
        RepositoryVerticleTest.ASTO.save(new Key.From("alice/conda.yml"), new byte[]{});
        vertx.deployVerticle(
            new RepositoryVerticle(
                new ManageRepoSettings(RepositoryVerticleTest.ASTO), "org",
                RepositoryVerticleTest.port
            ),
            ctx.succeedingThenComplete()
        );
    }

    @Test
    void listsAllRepos(final Vertx vertx, final VertxTestContext ctx) {
        vertx.createHttpClient().request(
            HttpMethod.GET, RepositoryVerticleTest.port, "localhost", "/api/v1/repository/list"
        )
            .compose(req -> req.send().compose(HttpClientResponse::body))
            .onComplete(
                ctx.succeeding(
                    buffer -> ctx.verify(
                        () -> {
                            MatcherAssert.assertThat(
                                buffer.toJsonArray().stream().collect(Collectors.toList()),
                                Matchers.containsInAnyOrder(
                                    "alice/rpm-local", "artipie/docker-repo", "alice/conda"
                                )
                            );
                            ctx.completeNow();
                        }
                    )
                )
        );
    }

    @Test
    void listsUserRepos(final Vertx vertx, final VertxTestContext ctx) {
        vertx.createHttpClient().request(
            HttpMethod.GET, RepositoryVerticleTest.port,
            "localhost", "/api/v1/repository/list/alice"
        )
            .compose(req -> req.send().compose(HttpClientResponse::body))
            .onComplete(
                ctx.succeeding(
                    buffer -> ctx.verify(
                        () -> {
                            MatcherAssert.assertThat(
                                buffer.toJsonArray().stream().collect(Collectors.toList()),
                                Matchers.containsInAnyOrder("alice/rpm-local", "alice/conda")
                            );
                            ctx.completeNow();
                        }
                    )
                )
        );
    }
}
