/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.http.auth.Authentication;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
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
 * Test for authentication in Rest API for flat layout.
 * As users management API is the same for org and flat, layout,
 * it's tested only in {@link AuthRestOrgTest}.
 * @since 0.27
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class AuthRestFlatTest extends RestApiServerBase {

    /**
     * Name of the user.
     */
    private static final String NAME = "Max";

    /**
     * User password.
     */
    private static final String PASS = "madmax";

    /**
     * List of the GET requests, which do not require existence of any settings.
     */
    private static final Collection<TestRequest> GET_DATA = List.of(
        new TestRequest(HttpMethod.GET, "/api/v1/repository/list"),
        new TestRequest(HttpMethod.GET, "/api/v1/repository/my-npm/storages"),
        new TestRequest(HttpMethod.GET, "/api/v1/storages")
    );

    /**
     * List of the existing requests for flat layout.
     */
    private static final Collection<TestRequest> RQST = Stream.concat(
        Stream.of(
            new TestRequest(HttpMethod.GET, "/api/v1/repository/my-maven"),
            new TestRequest(HttpMethod.PUT, "/api/v1/repository/rpm"),
            new TestRequest(HttpMethod.DELETE, "/api/v1/repository/my-python"),
            new TestRequest(HttpMethod.PUT, "/api/v1/repository/bin-files/move"),
            new TestRequest(HttpMethod.PUT, "/api/v1/repository/my-go/storages/local"),
            new TestRequest(HttpMethod.DELETE, "/api/v1/repository/docker/storages/s3sto"),
            new TestRequest(HttpMethod.PUT, "/api/v1/storages/def"),
            new TestRequest(HttpMethod.DELETE, "/api/v1/storages/local-dir")
        ), AuthRestFlatTest.GET_DATA.stream()
    ).toList();

    @Test
    void returnsUnauthorizedWhenTokenIsAbsent(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        for (final TestRequest item : AuthRestFlatTest.RQST) {
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
        final AtomicReference<String> token = this.getToken(
            vertx, ctx, AuthRestFlatTest.NAME, AuthRestFlatTest.PASS
        );
        for (final TestRequest item : AuthRestFlatTest.GET_DATA) {
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
            this.getToken(vertx, ctx, AuthRestFlatTest.NAME, AuthRestFlatTest.PASS);
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
    void createsAndRemovesStorageAliasWithAuth(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final AtomicReference<String> token =
            this.getToken(vertx, ctx, AuthRestFlatTest.NAME, AuthRestFlatTest.PASS);
        final String path = "/api/v1/storages/new-alias";
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT, path,
                new JsonObject().put("type", "file").put("path", "/new/alias/path")
            ),
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

    @Override
    Authentication auth() {
        return new Authentication.Single(AuthRestFlatTest.NAME, AuthRestFlatTest.PASS);
    }

    @Override
    String layout() {
        return "flat";
    }
}
