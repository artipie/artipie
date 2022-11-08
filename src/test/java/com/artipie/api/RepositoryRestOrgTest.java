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
 * Test for {@link RepositoryRest} with `org` laout.
 * @since 0.26
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
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
    void getRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        getRepoReturnsOkIfRepositoryExists(
            vertx, ctx, new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void getRepoReturnsConflictIfRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        getRepoReturnsConflictIfRepositoryHasSettingsDuplicates(
            vertx,
            ctx,
            new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void getRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.getRepoReturnsNotFoundIfRepositoryDoesNotExist(
            vertx, ctx, new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void getRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            getRepoReturnsBadRequestIfRepositoryHasReservedName(
                vertx, ctx, new RepositoryName.Org(name, "Alice")
            );
        }
    }

    @Test
    void existsRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        existsRepoReturnsOkIfRepositoryExists(
            vertx, ctx, new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void existsRepoReturnsConflictIfRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        existsRepoReturnsConflictIfRepositoryHasSettingsDuplicates(
            vertx,
            ctx,
            new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void existsRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.existsRepoReturnsNotFoundIfRepositoryDoesNotExist(
            vertx, ctx, new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void existsRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            existsRepoReturnsBadRequestIfRepositoryHasReservedName(
                vertx, ctx, new RepositoryName.Org(name, "Alice")
            );
        }
    }

    @Test
    void createRepoReturnsOkIfRepoNoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        createRepoReturnsOkIfRepositoryNoExists(
            vertx, ctx, new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void updateRepoReturnsOkIfRepoAlreadyExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        updateRepoReturnsOkIfRepositoryAlreadyExists(
            vertx, ctx, new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void createRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            createRepoReturnsBadRequestIfRepositoryHasReservedName(
                vertx, ctx, new RepositoryName.Org(name, "Alice")
            );
        }
    }

    @Test
    void removeRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        removeRepoReturnsOkIfRepositoryExists(
            vertx, ctx, new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void removeRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.removeRepoReturnsNotFoundIfRepositoryDoesNotExist(
            vertx, ctx, new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void removeRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            removeRepoReturnsBadRequestIfRepositoryHasReservedName(
                vertx, ctx, new RepositoryName.Org(name, "Alice")
            );
        }
    }

    @Test
    void removeRepoReturnsOkIfRepoHasWrongStorageConfiguration(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        removeRepoReturnsOkIfRepositoryHasWrongStorageConfiguration(
            vertx, ctx, new RepositoryName.Org("docker", "Alice")
        );
    }

    @Test
    void removeRepoReturnsOkAndRepoIsRemovedIfRepoHasWrongConfiguration(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        removeRepoReturnsOkAndRepoIsRemovedIfRepositoryHasWrongConfiguration(
            vertx, ctx, new RepositoryName.Org("docker", "Alice")
        );
    }

    @Test
    void moveRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        moveRepoReturnsOkIfRepositoryExists(
            vertx,
            ctx,
            new RepositoryName.Org("docker-repo", "Alice"),
            new RepositoryName.Org("docker-repo-new", "Alice")
        );
    }

    @Test
    void moveRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        moveRepoReturnsNotFoundIfRepositoryDoesNotExist(
            vertx, ctx, new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void moveRepoReturnsConflictIfRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        moveRepoReturnsConflictIfRepositoryHasSettingsDuplicates(
            vertx,
            ctx,
            new RepositoryName.Org("docker-repo", "Alice")
        );
    }

    @Test
    void moveRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            moveRepoReturnsBadRequestIfRepositoryHasReservedName(
                vertx, ctx, new RepositoryName.Org(name, "Alice")
            );
        }
    }

    @Test
    void moveRepoReturnsBadRequestIfNewRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final RepositoryName rname = new RepositoryName.Org("doker-repo", "Alice");
        for (final String name : Set.of("_storages", "_permissions", "_credentials")) {
            moveRepoReturnsBadRequestIfNewRepositoryHasReservedName(
                vertx, ctx, rname, new RepositoryName.Org(name, "Alice")
            );
        }
    }

    @Test
    void moveRepoReturnsBadRequestIfNewRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final RepositoryName rname = new RepositoryName.Org("doker-repo", "Alice");
        final String newrname = "doker-repo-new";
        moveRepoReturnsBadRequestIfNewRepositoryHasSettingsDuplicates(vertx, ctx, rname, newrname);
    }

    @Override
    String layout() {
        return "org";
    }
}
