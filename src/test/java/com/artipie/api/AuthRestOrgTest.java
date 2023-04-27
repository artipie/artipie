/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.http.auth.Authentication;
import com.artipie.security.policy.CachedYamlPolicy;
import com.artipie.security.policy.Policy;
import com.artipie.settings.ArtipieSecurity;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for authentication in Rest API for org layout.
 * @since 0.27
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class AuthRestOrgTest extends RestApiServerBase {

    /**
     * Name of the user.
     */
    private static final String NAME = "Aladdin";

    /**
     * User password.
     */
    private static final String PASS = "opensesame";

    /**
     * List of the GET requests, which do not require existence of any settings.
     */
    private static final Collection<TestRequest> GET_DATA = List.of(
        new TestRequest(HttpMethod.GET, "/api/v1/users"),
        new TestRequest(HttpMethod.GET, "/api/v1/roles"),
        new TestRequest(HttpMethod.GET, "/api/v1/repository/list"),
        new TestRequest(HttpMethod.GET, "/api/v1/repository/Cate/my-npm/storages"),
        new TestRequest(HttpMethod.GET, "/api/v1/storages/Oleg"),
        new TestRequest(HttpMethod.GET, "/api/v1/storages")
    );

    /**
     * List of the existing requests for org layout.
     */
    private static final Collection<TestRequest> RQST = Stream.concat(
        Stream.of(
            new TestRequest(HttpMethod.PUT, "/api/v1/users/Mark"),
            new TestRequest(HttpMethod.GET, "/api/v1/users/Alice"),
            new TestRequest(HttpMethod.DELETE, "/api/v1/users/Justine"),
            new TestRequest(HttpMethod.POST, "/api/v1/users/David/alter/password"),
            new TestRequest(HttpMethod.POST, "/api/v1/users/David/enable"),
            new TestRequest(HttpMethod.POST, "/api/v1/users/David/disable"),
            new TestRequest(HttpMethod.GET, "/api/v1/roles/java-dev"),
            new TestRequest(HttpMethod.PUT, "/api/v1/roles/admin"),
            new TestRequest(HttpMethod.DELETE, "/api/v1/roles/tester"),
            new TestRequest(HttpMethod.POST, "/api/v1/roles/tester/enable"),
            new TestRequest(HttpMethod.POST, "/api/v1/roles/tester/disable"),
            new TestRequest(HttpMethod.GET, "/api/v1/repository/list/John"),
            new TestRequest(HttpMethod.GET, "/api/v1/repository/Olga/my-maven"),
            new TestRequest(HttpMethod.PUT, "/api/v1/repository/Jane/rpm"),
            new TestRequest(HttpMethod.DELETE, "/api/v1/repository/Sasha/my-python"),
            new TestRequest(HttpMethod.PUT, "/api/v1/repository/Alex/bin-files/move"),
            new TestRequest(HttpMethod.PUT, "/api/v1/repository/Katie/my-go/storages/local"),
            new TestRequest(HttpMethod.DELETE, "/api/v1/repository/Ann/docker/storages/s3sto"),
            new TestRequest(HttpMethod.PUT, "/api/v1/storages/def"),
            new TestRequest(HttpMethod.DELETE, "/api/v1/storages/local-dir"),
            new TestRequest(HttpMethod.PUT, "/api/v1/storages/Andrew/s3-shared"),
            new TestRequest(HttpMethod.DELETE, "/api/v1/storages/Dmitrii/s3-amazon")
        ), AuthRestOrgTest.GET_DATA.stream()
    ).toList();

    @Test
    void returnsUnauthorizedWhenTokenIsAbsent(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        for (final TestRequest item : AuthRestOrgTest.RQST) {
            this.requestAndAssert(
                vertx, ctx, item, Optional.empty(),
                response -> MatcherAssert.assertThat(
                    String.format("%s failed", item),
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.UNAUTHORIZED_401)
                )
            );
        }
    }

    @Test
    void returnsOkWhenTokenIsPresent(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, AuthRestOrgTest.NAME, AuthRestOrgTest.PASS);
        for (final TestRequest item : AuthRestOrgTest.GET_DATA) {
            this.requestAndAssert(
                vertx, ctx, item, Optional.of(token.get()),
                response -> MatcherAssert.assertThat(
                    String.format("%s failed", item),
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                )
            );
        }
    }

    @Test
    void createsAndRemovesRepoWithAuth(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, AuthRestOrgTest.NAME, AuthRestOrgTest.PASS);
        final String path = "/api/v1/repository/john/my-docker";
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT, path,
                new JsonObject().put(
                    "repo", new JsonObject().put("type", "fs").put("storage", "def")
                )
            ), Optional.of(token.get()),
            resp -> MatcherAssert.assertThat(
                resp.statusCode(),
                new IsEqual<>(HttpStatus.OK_200)
            )
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, path), Optional.of(token.get()),
            resp -> MatcherAssert.assertThat(
                resp.statusCode(),
                new IsEqual<>(HttpStatus.OK_200)
            )
        );
    }

    @Test
    void createsAndRemovesUserWithAuth(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, AuthRestOrgTest.NAME, AuthRestOrgTest.PASS);
        final String path = "/api/v1/users/Alice";
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT, path,
                new JsonObject().put("type", "plain").put("pass", "wonderland")
                    .put("roles", JsonArray.of("readers", "tags"))
            ), Optional.of(token.get()),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.CREATED_201)
            )
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, path), Optional.of(token.get()),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.OK_200)
            )
        );
    }

    @Test
    void createsAndRemovesRoleWithAuth(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, AuthRestOrgTest.NAME, AuthRestOrgTest.PASS);
        final String path = "/api/v1/roles/admin";
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT, path,
                new JsonObject().put(
                    "permissions", new JsonObject().put("all_permission", new JsonObject())
                )
            ), Optional.of(token.get()),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.CREATED_201)
            )
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, path), Optional.of(token.get()),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.OK_200)
            )
        );
    }

    @Test
    void createsAndRemovesStorageAliasWithAuth(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, AuthRestOrgTest.NAME, AuthRestOrgTest.PASS);
        final String path = "/api/v1/storages/Jane/new-alias";
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT, path,
                new JsonObject().put("type", "file").put("path", "jane/new/alias/path")
            ), Optional.of(token.get()),
            resp -> MatcherAssert.assertThat(
                resp.statusCode(),
                new IsEqual<>(HttpStatus.CREATED_201)
            )
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, path), Optional.of(token.get()),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.OK_200)
            )
        );
    }

    @Test
    void returnUnauthorizedWhenOldPasswordIsNotCorrectOnAlterPassword(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, AuthRestOrgTest.NAME, AuthRestOrgTest.PASS);
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.POST,
                String.format("/api/v1/users/%s/alter/password", AuthRestOrgTest.NAME),
                new JsonObject().put("old_pass", "abc123").put("new_type", "plain")
                    .put("new_pass", "xyz098")
            ), Optional.of(token.get()),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.UNAUTHORIZED_401)
            )
        );
    }

    /**
     * Artipie authentication.
     * @return Authentication instance.
     * @checkstyle AnonInnerLengthCheck (30 lines)
     */
    ArtipieSecurity auth() {
        return new ArtipieSecurity() {
            @Override
            public Authentication authentication() {
                return new Authentication.Single(AuthRestOrgTest.NAME, AuthRestOrgTest.PASS);
            }

            @Override
            public Policy<?> policy() {
                final BlockingStorage asto = new BlockingStorage(AuthRestOrgTest.super.ssto);
                asto.save(
                    new Key.From(String.format("users/%s.yaml", AuthRestOrgTest.NAME)),
                    String.join(
                        "\n",
                        "permissions:",
                        "  all_permission: {}"
                    ).getBytes(StandardCharsets.UTF_8)
                );
                // @checkstyle MagicNumberCheck (1 line)
                return new CachedYamlPolicy(asto, 60_000L);
            }

            @Override
            public Optional<Storage> policyStorage() {
                return Optional.of(AuthRestOrgTest.super.ssto);
            }
        };
    }

    @Override
    String layout() {
        return "org";
    }
}
