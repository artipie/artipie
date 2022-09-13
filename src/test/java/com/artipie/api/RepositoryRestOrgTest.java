/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for {@link RepositoryRest} with `org` laout.
 * @since 0.26
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RepositoryRestOrgTest extends RepositoryRestBaseTest {
    @Test
    void listsAllRepos(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(new Key.From("artipie/docker-repo.yaml"), new byte[0]);
        this.save(new Key.From("alice/rpm-local.yml"), new byte[0]);
        this.save(new Key.From("alice/conda.yml"), new byte[0]);
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/repository/list"),
            resp ->
                MatcherAssert.assertThat(
                    resp.body().toJsonArray().stream().collect(Collectors.toList()),
                    Matchers.containsInAnyOrder(
                        "alice/rpm-local", "artipie/docker-repo", "alice/conda"
                    )
                )
        );
    }

    @Test
    void listsUserRepos(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(new Key.From("artipie/docker-repo.yaml"), new byte[0]);
        this.save(new Key.From("alice/rpm-local.yml"), new byte[0]);
        this.save(new Key.From("alice/conda.yml"), new byte[0]);
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/repository/list/alice"),
            resp ->
                MatcherAssert.assertThat(
                    resp.body().toJsonArray().stream().collect(Collectors.toList()),
                    Matchers.containsInAnyOrder("alice/rpm-local", "alice/conda")
                )
        );
    }

    @Test
    void createUserRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        final JsonObject json = new JsonObject()
            .put(
                "repo", new JsonObject()
                    .put("type", "fs")
                    .put("storage", new JsonObject())
            );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.PUT, "/api/v1/repository/alice/newrepo", json),
            resp -> {
                MatcherAssert.assertThat(
                    resp.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    this.storage().exists(new Key.From("alice/newrepo.yaml")),
                    new IsEqual<>(true)
                );
            }
        );
    }

    @Test
    void createDuplicateUserRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(new Key.From("alice/newrepo.yaml"), new byte[0]);
        final JsonObject json = new JsonObject()
            .put(
                "repo", new JsonObject()
                    .put("type", "fs")
                    .put("storage", new JsonObject())
            );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.PUT, "/api/v1/repository/alice/newrepo", json),
            resp ->
                MatcherAssert.assertThat(
                    resp.statusCode(),
                    new IsEqual<>(HttpStatus.CONFLICT_409)
                )
        );
    }

    @Test
    void deleteRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(
            new Key.From("artipie/docker-repo.yaml"),
            this.repoSettings().getBytes(StandardCharsets.UTF_8)
        );
        final Key.From alpine = new Key.From("artipie/docker-repo/alpine.img");
        this.getData().save(alpine, new byte[]{});
        final Key.From python = new Key.From("artipie/docker-repo/python.img");
        this.getData().save(python, new byte[]{});
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.DELETE,
                "/api/v1/repository/artipie/docker-repo"
            ),
            res -> {
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    waitCondition(
                        () ->
                            !this.storage().exists(new Key.From("artipie/docker-repo.yaml"))
                    ),
                    new IsEqual<>(true)
                );
                MatcherAssert.assertThat(
                    waitCondition(() -> !this.getData().exists(alpine)),
                    new IsEqual<>(true)
                );
                MatcherAssert.assertThat(
                    waitCondition(() -> !this.getData().exists(python)),
                    new IsEqual<>(true)
                );
            }
        );
    }

    @Test
    void deleteRepoNotfound(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.DELETE,
                "/api/v1/repository/artipie/docker-repo"
            ),
            res -> {
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                );
            }
        );
    }

    @Test
    void deleteReservedRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            this.requestAndAssert(
                vertx, ctx, new TestRequest(
                    HttpMethod.DELETE, String.format("/api/v1/repository/artipie/%s", name)
                ),
                res -> {
                    MatcherAssert.assertThat(
                        res.statusCode(),
                        new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                    );
                }
            );
        }
    }

    @Override
    String layout() {
        return "org";
    }
}
