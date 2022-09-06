/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for {@link RepositoryRest} with `org` laout.
 * @since 0.26
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RepositoryRestOrgTest extends RestApiServerBase {

    @Test
    void listsAllRepos(final Vertx vertx, final VertxTestContext ctx) throws ExecutionException,
        InterruptedException, TimeoutException {
        this.save(new Key.From("artipie/docker-repo.yaml"), new byte[0]);
        this.save(new Key.From("alice/rpm-local.yml"), new byte[0]);
        this.save(new Key.From("alice/conda.yml"), new byte[0]);
        vertx.createHttpClient()
            .request(
                HttpMethod.GET, this.port(), RestApiServerBase.HOST, "/api/v1/repository/list"
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
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RestApiServerBase.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    void listsUserRepos(final Vertx vertx, final VertxTestContext ctx)
        throws ExecutionException, InterruptedException, TimeoutException {
        this.save(new Key.From("artipie/docker-repo.yaml"), new byte[0]);
        this.save(new Key.From("alice/rpm-local.yml"), new byte[0]);
        this.save(new Key.From("alice/conda.yml"), new byte[0]);
        vertx.createHttpClient().request(
            HttpMethod.GET, this.port(), RestApiServerBase.HOST, "/api/v1/repository/list/alice"
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
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RestApiServerBase.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    void createUserRepo(final Vertx vertx, final VertxTestContext ctx)
        throws ExecutionException, InterruptedException, TimeoutException {
        final JsonObject json = new JsonObject()
            .put(
                "repo", new JsonObject()
                    .put("type", "fs")
                    .put("storage", new JsonObject())
            );
        WebClient.create(vertx)
            .put(this.port(), RestApiServerBase.HOST, "/api/v1/repository/alice/newrepo")
            .sendJsonObject(json)
            .onSuccess(
                res -> {
                    MatcherAssert.assertThat(
                        res.statusCode(),
                        // @checkstyle MagicNumberCheck (1 line)
                        Matchers.is(200)
                    );
                    MatcherAssert.assertThat(
                        this.storage().exists(new Key.From("alice/newrepo.yml")),
                        Matchers.is(true)
                    );
                    ctx.completeNow();
                }
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RestApiServerBase.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    void createDuplicateUserRepo(final Vertx vertx, final VertxTestContext ctx)
        throws ExecutionException, InterruptedException, TimeoutException {
        this.save(new Key.From("alice/newrepo.yaml"), new byte[0]);
        final JsonObject json = new JsonObject()
            .put(
                "repo", new JsonObject()
                    .put("type", "fs")
                    .put("storage", new JsonObject())
            );
        WebClient.create(vertx)
            .put(this.port(), RestApiServerBase.HOST, "/api/v1/repository/alice/newrepo")
            .sendJsonObject(json)
            .onSuccess(
                res -> {
                    MatcherAssert.assertThat(
                        res.statusCode(),
                        // @checkstyle MagicNumberCheck (1 line)
                        Matchers.is(409)
                    );
                    ctx.completeNow();
                }
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RestApiServerBase.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    @Override
    String layout() {
        return "org";
    }
}
