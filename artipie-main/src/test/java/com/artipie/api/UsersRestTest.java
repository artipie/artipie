/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.misc.UncheckedConsumer;
import com.artipie.test.TestArtipieCaches;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link UsersRest}.
 */
final class UsersRestTest extends RestApiServerBase {

    @Test
    void listsUsers(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("users/Alice.yaml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: qwerty",
                "roles:",
                "  - readers",
                "permissions:",
                "  adapter_basic_permissions:",
                "    repo1:",
                "      - write"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.saveIntoSecurityStorage(
            new Key.From("users/Bob.yml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: qwerty",
                "email: \"bob@example.com\"",
                "roles:",
                "  - admin"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/users"),
            new UncheckedConsumer<>(
                response -> JSONAssert.assertEquals(
                    response.body().toString(),
                    "[{\"name\":\"Alice\",\"roles\":[\"readers\"], \"permissions\":{\"adapter_basic_permissions\":{\"repo1\":[\"write\"]}}},{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"roles\":[\"admin\"]}]",
                    false
                )
            )
        );
    }

    @Test
    void getsUser(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("users/John.yml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: xyz",
                "email: \"john@example.com\"",
                "roles:",
                "  - readers",
                "  - tags"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/users/John"),
            new UncheckedConsumer<>(
                response -> JSONAssert.assertEquals(
                    response.body().toString(),
                    "{\"name\":\"John\",\"email\":\"john@example.com\",\"roles\":[\"readers\",\"tags\"]}",
                    false
                )
            )
        );
    }

    @Test
    void returnsNotFoundIfUserDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/users/Jane"),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.NOT_FOUND_404)
            )
        );
    }

    @Test
    void altersUser(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("users/Mark.yml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: xyz",
                "email: \"any@example.com\"",
                "roles:",
                "  - reader"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT, "/api/v1/users/Mark",
                new JsonObject().put("type", "plain").put("pass", "qwerty")
                    .put("email", "mark@example.com")
            ),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.CREATED_201)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.securityStorage().value(new Key.From("users/Mark.yml")),
                        StandardCharsets.UTF_8
                    ),
                    new StringContains(
                        String.join(
                            System.lineSeparator(),
                            "type: plain",
                            "pass: qwerty",
                            "email: \"mark@example.com\""
                        )
                    )
                );
            }
        );
    }

    @Test
    void addsUser(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT, "/api/v1/users/Alice",
                new JsonObject().put("type", "plain").put("pass", "wonderland")
                    .put("roles", JsonArray.of("readers", "tags"))
                    .put(
                        "permissions",
                        new JsonObject().put(
                            "adapter_basic_permissions",
                            new JsonObject().put("maven-repo", JsonArray.of("read", "write"))
                        )
                    )
            ),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.CREATED_201)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.securityStorage().value(new Key.From("users/Alice.yml")),
                        StandardCharsets.UTF_8
                    ),
                    new StringContains(
                        String.join(
                            System.lineSeparator(),
                            "type: plain",
                            "pass: wonderland",
                            "roles:",
                            "  - readers",
                            "  - tags",
                            "permissions:",
                            "  adapter_basic_permissions:",
                            "    \"maven-repo\":",
                            "      - read",
                            "      - write"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Auth cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wereUsersInvalidated()
                );
                MatcherAssert.assertThat(
                    "Policy cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasPolicyInvalidated()
                );
            }
        );
    }

    @Test
    void returnsNotFoundIfUserDoesNotExistOnDelete(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, "/api/v1/users/Jane"),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.NOT_FOUND_404)
            )
        );
    }

    @Test
    void returnsNotFoundIfUserDoesNotExistOnEnable(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.POST, "/api/v1/users/Jane/enable"),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.NOT_FOUND_404)
            )
        );
    }

    @Test
    void returnsNotFoundIfUserDoesNotExistOnDisable(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.POST, "/api/v1/users/Jane/disable"),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.NOT_FOUND_404)
            )
        );
    }

    @Test
    void removesUser(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("users/Alice.yaml"),
            new byte[]{}
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, "/api/v1/users/Alice"),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    this.securityStorage().exists(new Key.From("users/Alice.yaml")),
                    new IsEqual<>(false)
                );
                MatcherAssert.assertThat(
                    "Auth cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wereUsersInvalidated()
                );
                MatcherAssert.assertThat(
                    "Policy cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasPolicyInvalidated()
                );
            }
        );
    }

    @Test
    void altersUserPassword(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        final String old = "abc123";
        this.saveIntoSecurityStorage(
            new Key.From("users/Mark.yml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: abc123",
                "email: any@example.com",
                "roles:",
                "  - reader"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.POST, "/api/v1/users/Mark/alter/password",
                new JsonObject().put("old_pass", old).put("new_type", "plain")
                    .put("new_pass", "xyz098")
            ),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.securityStorage().value(new Key.From("users/Mark.yml")),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "type: plain",
                            "pass: xyz098",
                            "email: \"any@example.com\"",
                            "roles:",
                            "  - reader"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Auth cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wereUsersInvalidated()
                );
            }
        );
    }

    @Test
    void returnsNotFoundWhenUserDoesNotExistsOnAlterPassword(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.POST, "/api/v1/users/Jane/alter/password",
                new JsonObject().put("old_pass", "any_pass").put("new_type", "plain")
                    .put("new_pass", "another_pass")
            ),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.NOT_FOUND_404)
            )
        );
    }

    @Test
    void enablesUser(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("users/Mark.yml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: abc123",
                "email: any@example.com",
                "enabled: false",
                "roles:",
                "  - reader"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.POST, "/api/v1/users/Mark/enable"),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.securityStorage().value(new Key.From("users/Mark.yml")),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "type: plain",
                            "pass: abc123",
                            "email: \"any@example.com\"",
                            "enabled: true",
                            "roles:",
                            "  - reader"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Auth cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wereUsersInvalidated()
                );
                MatcherAssert.assertThat(
                    "Policy cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasPolicyInvalidated()
                );
            }
        );
    }

    @Test
    void disablesUser(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("users/John.yml"),
            String.join(
                System.lineSeparator(),
                "type: plain",
                "pass: abc123",
                "email: any@example.com"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.POST, "/api/v1/users/John/disable"),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.securityStorage().value(new Key.From("users/John.yml")),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "type: plain",
                            "pass: abc123",
                            "email: \"any@example.com\"",
                            "enabled: false"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Auth cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wereUsersInvalidated()
                );
                MatcherAssert.assertThat(
                    "Policy cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasPolicyInvalidated()
                );
            }
        );
    }

}
