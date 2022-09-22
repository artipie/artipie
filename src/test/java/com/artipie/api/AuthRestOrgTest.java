/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.http.auth.Authentication;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.VertxTestContext;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for authentication in Rest API for org layout.
 * @since 0.27
 */
public final class AuthRestOrgTest extends RestApiServerBase {

    /**
     * List of the existing requests for org layout.
     */
    private static final Collection<RestApiServerBase.TestRequest> RQST = List.of(
        //@checkstyle LineLengthCheck (20 lines)
        new RestApiServerBase.TestRequest(HttpMethod.GET, "/api/v1/users"),
        new RestApiServerBase.TestRequest(HttpMethod.PUT, "/api/v1/users/Mark"),
        new RestApiServerBase.TestRequest(HttpMethod.GET, "/api/v1/users/Alice"),
        new RestApiServerBase.TestRequest(HttpMethod.DELETE, "/api/v1/users/Justine"),
        new RestApiServerBase.TestRequest(HttpMethod.GET, "/api/v1/repository/list"),
        new RestApiServerBase.TestRequest(HttpMethod.GET, "/api/v1/repository/list/John"),
        new RestApiServerBase.TestRequest(HttpMethod.GET, "/api/v1/repository/Olga/my-maven"),
        new RestApiServerBase.TestRequest(HttpMethod.PUT, "/api/v1/repository/Jane/rpm"),
        new RestApiServerBase.TestRequest(HttpMethod.DELETE, "/api/v1/repository/Sasha/my-python"),
        new RestApiServerBase.TestRequest(HttpMethod.PUT, "/api/v1/repository/Alex/bin-files/move"),
        new RestApiServerBase.TestRequest(HttpMethod.GET, "/api/v1/repository/Cate/my-npm/storages"),
        new RestApiServerBase.TestRequest(HttpMethod.PUT, "/api/v1/repository/Katie/my-go/storages/local"),
        new RestApiServerBase.TestRequest(HttpMethod.DELETE, "/api/v1/repository/Ann/docker/storages/s3sto"),
        new RestApiServerBase.TestRequest(HttpMethod.GET, "/api/v1/storages"),
        new RestApiServerBase.TestRequest(HttpMethod.PUT, "/api/v1/storages/def"),
        new RestApiServerBase.TestRequest(HttpMethod.DELETE, "/api/v1/storages/local-dir"),
        new RestApiServerBase.TestRequest(HttpMethod.GET, "/api/v1/storages/Oleg"),
        new RestApiServerBase.TestRequest(HttpMethod.PUT, "/api/v1/storages/Andrew/s3-shared"),
        new RestApiServerBase.TestRequest(HttpMethod.DELETE, "/api/v1/storages/Dmitrii/s3-amazon")
    );

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

    @Override
    Authentication auth() {
        return new Authentication.Single("Aladdin", "opensesame");
    }

    @Override
    String layout() {
        return "org";
    }
}
