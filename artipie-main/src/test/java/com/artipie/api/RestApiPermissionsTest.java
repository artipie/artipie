/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.http.auth.AuthUser;
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
 * Test for permissions for rest api.
 * @since 0.30
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RestApiPermissionsTest extends RestApiServerBase {

    /**
     * Name of the user.
     */
    private static final String NAME = "artipie";

    /**
     * User password.
     */
    private static final String PASS = "whatever";

    /**
     * List of the GET requests, which do not require existence of any settings.
     */
    private static final Collection<TestRequest> GET_DATA = List.of(
        new TestRequest(HttpMethod.GET, "/api/v1/roles"),
        new TestRequest(HttpMethod.GET, "/api/v1/users"),
        new TestRequest(HttpMethod.GET, "/api/v1/repository/list"),
        new TestRequest(HttpMethod.GET, "/api/v1/repository/my-npm/storages"),
        new TestRequest(HttpMethod.GET, "/api/v1/storages")
    );

    /**
     * List of the existing requests.
     */
    private static final Collection<TestRequest> RQST = Stream.concat(
        Stream.of(
            // @checkstyle LineLengthCheck (500 lines)
            new TestRequest(HttpMethod.PUT, "/api/v1/users/Mark", new JsonObject().put("type", "plain").put("pass", "abc123")),
            new TestRequest(HttpMethod.GET, "/api/v1/users/Alice"),
            new TestRequest(HttpMethod.DELETE, "/api/v1/users/Justine"),
            new TestRequest(HttpMethod.POST, "/api/v1/users/David/alter/password", new JsonObject().put("old_pass", "any").put("new_pass", "ane").put("new_type", "plain")),
            new TestRequest(HttpMethod.POST, "/api/v1/users/David/enable"),
            new TestRequest(HttpMethod.POST, "/api/v1/users/David/disable"),
            new TestRequest(HttpMethod.GET, "/api/v1/roles/java-dev"),
            new TestRequest(HttpMethod.PUT, "/api/v1/roles/admin", new JsonObject().put("permissions", new JsonObject().put("all_permission", new JsonObject()))),
            new TestRequest(HttpMethod.DELETE, "/api/v1/roles/tester"),
            new TestRequest(HttpMethod.POST, "/api/v1/roles/tester/enable"),
            new TestRequest(HttpMethod.POST, "/api/v1/roles/tester/disable"),
            new TestRequest(HttpMethod.GET, "/api/v1/repository/my-maven"),
            new TestRequest(HttpMethod.PUT, "/api/v1/repository/rpm", new JsonObject().put("repo", new JsonObject())),
            new TestRequest(HttpMethod.DELETE, "/api/v1/repository/my-python"),
            new TestRequest(HttpMethod.PUT, "/api/v1/repository/bin-files/move", new JsonObject().put("new_name", "any")),
            new TestRequest(HttpMethod.PUT, "/api/v1/repository/my-go/storages/local", new JsonObject().put("alias", "local").put("type", "file")),
            new TestRequest(HttpMethod.DELETE, "/api/v1/repository/docker/storages/s3sto"),
            new TestRequest(HttpMethod.PUT, "/api/v1/storages/def", new JsonObject().put("alias", "def").put("type", "file")),
            new TestRequest(HttpMethod.DELETE, "/api/v1/storages/local-dir")
        ), RestApiPermissionsTest.GET_DATA.stream()
    ).toList();

    @Test
    void returnsForbiddenIfUserDoesNotHavePermissions(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token = this.getToken(vertx, ctx, "john", "whatever");
        for (final TestRequest item : RestApiPermissionsTest.RQST) {
            this.requestAndAssert(
                vertx, ctx, item, Optional.of(token.get()),
                response -> MatcherAssert.assertThat(
                    String.format("%s failed", item),
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.FORBIDDEN_403)
                )
            );
        }
    }

    @Test
    void returnsOkIfUserHasPermissions(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token = this.getToken(vertx, ctx, "artipie", "whatever");
        for (final TestRequest item : RestApiPermissionsTest.GET_DATA) {
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
    void createsAndRemovesRepoWithPerms(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, RestApiPermissionsTest.NAME, RestApiPermissionsTest.PASS);
        final String path = "/api/v1/repository/my-docker";
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
    void createsAndRemovesUserWithPerms(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, RestApiPermissionsTest.NAME, RestApiPermissionsTest.PASS);
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
    void createsAndRemovesRoleWithPerms(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, RestApiPermissionsTest.NAME, RestApiPermissionsTest.PASS);
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
    void createsAndRemovesStorageAliasWithPerms(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, RestApiPermissionsTest.NAME, RestApiPermissionsTest.PASS);
        final String path = "/api/v1/storages/new-alias";
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT, path,
                new JsonObject().put("type", "file").put("path", "new/alias/path")
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

    /**
     * Artipie authentication.
     * @return Authentication instance.
     * @checkstyle AnonInnerLengthCheck (30 lines)
     */
    ArtipieSecurity auth() {
        return new ArtipieSecurity() {
            @Override
            public Authentication authentication() {
                return (name, pswd) -> Optional.of(new AuthUser(name, "test"));
            }

            @Override
            public Policy<?> policy() {
                final BlockingStorage blsto =
                    new BlockingStorage(RestApiPermissionsTest.super.ssto);
                blsto.save(
                    new Key.From("users/artipie.yaml"),
                    String.join(
                        "\n",
                        "permissions:",
                        "  api_storage_alias_permissions:",
                        "    - read",
                        "    - create",
                        "    - delete",
                        "  api_repository_permissions:",
                        "    - *",
                        "  api_role_permissions:",
                        "    - read",
                        "    - create",
                        "    - update",
                        "    - delete",
                        "    - enable",
                        "  api_user_permissions:",
                        "    - *"
                    ).getBytes(StandardCharsets.UTF_8)
                );
                // @checkstyle MagicNumberCheck (500 lines)
                return new CachedYamlPolicy(blsto, 60_000L);
            }

            @Override
            public Optional<Storage> policyStorage() {
                return Optional.of(RestApiPermissionsTest.super.ssto);
            }
        };
    }
}
