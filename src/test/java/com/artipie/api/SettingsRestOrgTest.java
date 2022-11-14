/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for {@link SettingsRest} with 'org' layout.
 * @since 0.27
 */
@ExtendWith(VertxExtension.class)
public final class SettingsRestOrgTest extends SettingsRestBaseTest {
    @Test
    void returnsPortAndStatusCodeOkForOrgLayout(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.returnsPortAndStatusCodeOk(vertx, ctx);
    }

    @Test
    void returnsFlatLayoutAndStatusCodeOk(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.returnsLayoutAndStatusCodeOk(vertx, ctx, this.layout());
    }

    @Override
    String layout() {
        return "org";
    }
}
