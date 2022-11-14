/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for {@link SettingsRest}.
 * @since 0.27
 * @checkstyle DesignForExtensionCheck (500 lines)
 */
@ExtendWith(VertxExtension.class)
public abstract class SettingsRestBaseTest extends RestApiServerBase {
    protected void returnsLayoutAndStatusCodeOk(final Vertx vertx, final VertxTestContext ctx,
        final String expected) throws Exception {
        this.requestAndAssert(
            vertx,
            ctx,
            new TestRequest("/api/v1/settings/layout"),
            res -> {
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    res.bodyAsJsonObject().getString("layout"),
                    new IsEqual<>(expected)
                );
            }
        );
    }

    protected void returnsPortAndStatusCodeOk(final Vertx vertx, final VertxTestContext ctx)
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
