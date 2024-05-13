/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Test for {@link SettingsRest}.
 * @since 0.27
 */
@ExtendWith(VertxExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public final class SettingsRestTest extends RestApiServerBase {

    @Test
    void returnsPortAndStatusCodeOk(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx,
            ctx,
            new TestRequest("/api/v1/settings/port"),
            res -> {
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    res.bodyAsJsonObject().getInteger("port"),
                    new IsEqual<>(this.port())
                );
            }
        );
    }
}
