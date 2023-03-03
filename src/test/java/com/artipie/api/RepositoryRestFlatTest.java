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
    void getRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        getRepoReturnsOkIfRepositoryExists(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void getRepoReturnsConflictIfRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        getRepoReturnsConflictIfRepositoryHasSettingsDuplicates(
            vertx, ctx, new RepositoryName.Flat("docker-repo")
        );
    }

    @Test
    void getRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        this.getRepoReturnsNotFoundIfRepositoryDoesNotExist(
            vertx, ctx, new RepositoryName.Flat("docker-repo")
        );
    }

    @Test
    void getRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages")) {
            getRepoReturnsBadRequestIfRepositoryHasReservedName(
                vertx, ctx, new RepositoryName.Flat(name)
            );
        }
    }

    @Test
    void existsRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        existsRepoReturnsOkIfRepositoryExists(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void existsRepoReturnsConflictIfRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        existsRepoReturnsConflictIfRepositoryHasSettingsDuplicates(
            vertx, ctx, new RepositoryName.Flat("docker-repo")
        );
    }

    @Test
    void existsRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.existsRepoReturnsNotFoundIfRepositoryDoesNotExist(
            vertx, ctx, new RepositoryName.Flat("docker-repo")
        );
    }

    @Test
    void existsRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages")) {
            existsRepoReturnsBadRequestIfRepositoryHasReservedName(
                vertx, ctx, new RepositoryName.Flat(name)
            );
        }
    }

    @Test
    void createRepoReturnsOkIfRepoNoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        createRepoReturnsOkIfRepositoryNoExists(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void updateRepoReturnsOkIfRepoAlreadyExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        updateRepoReturnsOkIfRepositoryAlreadyExists(
            vertx, ctx, new RepositoryName.Flat("docker-repo")
        );
    }

    @Test
    void createRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages")) {
            createRepoReturnsBadRequestIfRepositoryHasReservedName(
                vertx, ctx, new RepositoryName.Flat(name)
            );
        }
    }

    @Test
    void removeRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        removeRepoReturnsOkIfRepositoryExists(vertx, ctx, new RepositoryName.Flat("docker-repo"));
    }

    @Test
    void removeRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.removeRepoReturnsNotFoundIfRepositoryDoesNotExist(
            vertx, ctx, new RepositoryName.Flat("docker-repo")
        );
    }

    @Test
    void removeRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages")) {
            removeRepoReturnsBadRequestIfRepositoryHasReservedName(
                vertx, ctx, new RepositoryName.Flat(name)
            );
        }
    }

    @Test
    void removeRepoReturnsOkIfRepoHasWrongStorageConfiguration(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        removeRepoReturnsOkIfRepositoryHasWrongStorageConfiguration(
            vertx, ctx, new RepositoryName.Flat("docker")
        );
    }

    @Test
    void removeRepoReturnsOkAndRepoIsRemovedIfRepoHasWrongConfiguration(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        removeRepoReturnsOkAndRepoIsRemovedIfRepositoryHasWrongConfiguration(
            vertx, ctx, new RepositoryName.Flat("docker")
        );
    }

    @Test
    void moveRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        moveRepoReturnsOkIfRepositoryExists(
            vertx,
            ctx,
            new RepositoryName.Flat("docker-repo"),
            new RepositoryName.Flat("docker-repo-new")
        );
    }

    @Test
    void moveRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        moveRepoReturnsNotFoundIfRepositoryDoesNotExist(
            vertx, ctx, new RepositoryName.Flat("docker-repo")
        );
    }

    @Test
    void moveRepoReturnsConflictIfRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        moveRepoReturnsConflictIfRepositoryHasSettingsDuplicates(
            vertx, ctx, new RepositoryName.Flat("docker-repo")
        );
    }

    @Test
    void moveRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages")) {
            moveRepoReturnsBadRequestIfRepositoryHasReservedName(
                vertx, ctx, new RepositoryName.Flat(name)
            );
        }
    }

    @Test
    void moveRepoReturnsBadRequestIfNewRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final RepositoryName rname = new RepositoryName.Flat("doker-repo");
        for (final String name : Set.of("_storages")) {
            moveRepoReturnsBadRequestIfNewRepositoryHasReservedName(
                vertx, ctx, rname, new RepositoryName.Flat(name)
            );
        }
    }

    @Test
    void moveRepoReturnsBadRequestIfNewRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final RepositoryName rname = new RepositoryName.Flat("doker-repo");
        final String newrname = "doker-repo-new";
        moveRepoReturnsBadRequestIfNewRepositoryHasSettingsDuplicates(vertx, ctx, rname, newrname);
    }

    @Override
    String layout() {
        return "flat";
    }
}
