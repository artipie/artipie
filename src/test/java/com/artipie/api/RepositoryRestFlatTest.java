/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for {@link RepositoryRest} with `flat` layout.
 * @since 0.26
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
final class RepositoryRestFlatTest extends RepositoryRestBaseTest {
    @Test
    void listsRepos(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(new Key.From("docker-repo.yaml"), new byte[0]);
        this.save(new Key.From("rpm-local.yml"), new byte[0]);
        this.save(new Key.From("conda-remote.yml"), new byte[0]);
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/repository/list"),
            resp -> MatcherAssert.assertThat(
                resp.body().toJsonArray().stream().collect(Collectors.toList()),
                Matchers.containsInAnyOrder(
                    "rpm-local", "docker-repo", "conda-remote"
                )
            )
        );
    }

    @Test
    void getRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        getRepository(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void getRepoWithDuplicatesSettings(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        getRepositoryWithDuplicatesSettings(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void getRepoNotfound(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.getRepositoryNotfound(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void getReservedRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            getReservedRepository(vertx, ctx, new RepositoryName.Flat(name));
        }
    }

    @Test
    void createRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        createRepository(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void createDuplicateRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        createDuplicateRepository(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void createReservedRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            createReservedRepository(vertx, ctx, new RepositoryName.Flat(name));
        }
    }

    @Test
    void deleteRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        deleteRepository(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void deleteRepoNotfound(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.deleteRepositoryNotfound(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void deleteReservedRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            deleteReservedRepository(vertx, ctx, new RepositoryName.Flat(name));
        }
    }

    @Test
    void moveRepo(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        moveRepository(
            vertx,
            ctx,
            new RepositoryName.Flat("docker-repo"),
            new RepositoryName.Flat("docker-repo-new")
        );
    }

    @Test
    void moveRepoNotFound(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        moveRepositoryNotfound(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void moveRepoWithDuplicatesSettings(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        moveRepositoryWithDuplicatesSettings(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void moveRepositoryReservedRepo(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            moveRepositoryReservedRepo(vertx, ctx, new RepositoryName.Flat(name));
        }
    }

    @Override
    String layout() {
        return "flat";
    }
}
