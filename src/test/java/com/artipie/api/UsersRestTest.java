/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.misc.UncheckedConsumer;
import com.artipie.settings.CredsConfigYaml;
import com.artipie.settings.users.PasswordFormat;
import com.artipie.test.TestArtipieCaches;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link UsersRest}.
 * @since 0.27
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class UsersRestTest extends RestApiServerBase {

    @Test
    void listsUsers(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(
            new Key.From(ManageUsersTest.KEY),
            new CredsConfigYaml().withUserAndGroups("Alice", List.of("readers")).withFullInfo(
                "Bob", PasswordFormat.PLAIN, "xyz", "bob@example.com", Set.of("admin")
            ).toString().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/users"),
            new UncheckedConsumer<>(
                response -> JSONAssert.assertEquals(
                    response.body().toString(),
                    // @checkstyle LineLengthCheck (1 line)
                    "[{\"name\":\"Alice\",\"groups\":[\"readers\"]},{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"groups\":[\"admin\"]}]",
                    false
                )
            )
        );
    }

    @Test
    void getsUser(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(
            new Key.From(ManageUsersTest.KEY),
            new CredsConfigYaml().withUsers("Mark")
                .withFullInfo(
                    "John", PasswordFormat.PLAIN, "231", "john@example.com",
                    Set.of("readers", "tags")
                ).toString().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/users/John"),
            new UncheckedConsumer<>(
                response -> JSONAssert.assertEquals(
                    response.body().toString(),
                    // @checkstyle LineLengthCheck (1 line)
                    "{\"name\":\"John\",\"email\":\"john@example.com\",\"groups\":[\"readers\",\"tags\"]}",
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
        this.save(
            new Key.From(ManageUsersTest.KEY),
            new CredsConfigYaml().withUsers("Mark").toString().getBytes(StandardCharsets.UTF_8)
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
                    new String(this.storage().value(ManageUsersTest.KEY), StandardCharsets.UTF_8),
                    new StringContains(
                        String.join(
                            System.lineSeparator(),
                            "  Mark:",
                            "    type: plain",
                            "    pass: qwerty",
                            "    email: mark@example.com"
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
                    .put("groups", JsonArray.of("readers", "tags"))
            ),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.CREATED_201)
                );
                MatcherAssert.assertThat(
                    new String(this.storage().value(ManageUsersTest.KEY), StandardCharsets.UTF_8),
                    new StringContains(
                        String.join(
                            System.lineSeparator(),
                            "  Alice:",
                            "    type: plain",
                            "    pass: wonderland",
                            "    groups:",
                            "      - readers",
                            "      - tags"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Auth cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasInvalidated()
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
    void removesUser(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(
            new Key.From(ManageUsersTest.KEY),
            new CredsConfigYaml().withUsers("Mark", "Alice")
                .toString().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, "/api/v1/users/Alice"),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    new String(this.storage().value(ManageUsersTest.KEY), StandardCharsets.UTF_8),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "credentials:",
                            "  Mark:",
                            "    pass: 123",
                            "    type: plain"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Auth cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasInvalidated()
                );
            }
        );
    }

    @Test
    void altersUserPassword(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        final String old = "abc123";
        this.save(
            new Key.From(ManageUsersTest.KEY),
            new CredsConfigYaml().withUserAndPswd("Mark", PasswordFormat.PLAIN, old)
                .toString().getBytes(StandardCharsets.UTF_8)
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
                    new String(this.storage().value(ManageUsersTest.KEY), StandardCharsets.UTF_8),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "credentials:",
                            "  Mark:",
                            "    pass: xyz098",
                            "    type: plain"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Auth cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasInvalidated()
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

    @Override
    String layout() {
        return "org";
    }
}
