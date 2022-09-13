/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.misc.UncheckedConsumer;
import com.artipie.settings.CredsConfigYaml;
import com.artipie.settings.users.Users;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
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
                "Bob", Users.PasswordFormat.PLAIN, "xyz", "bob@example.com", Set.of("admin")
            ).toString().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/users"),
            new UncheckedConsumer<>(
                response -> JSONAssert.assertEquals(
                    response.body().toString(),
                    // @checkstyle LineLengthCheck (1 line)
                    "[{\"name\":\"Alice\",\"groups\":[\"readers\"]},{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"groups\":[\"admin\"]}]",
                    true
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
                    "John", Users.PasswordFormat.PLAIN, "231", "john@example.com",
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
                    true
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

    @Override
    String layout() {
        return "org";
    }
}
