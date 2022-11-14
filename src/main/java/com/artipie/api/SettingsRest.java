/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.eclipse.jetty.http.HttpStatus;

/**
 * REST API methods to manage Artipie settings.
 * @since 0.27
 */
public final class SettingsRest extends BaseRest {
    /**
     * Artipie layout.
     */
    private final String layout;

    /**
     * Artipie port.
     */
    private final int port;

    /**
     * Ctor.
     * @param port Artipie port
     * @param layout Artipie layout
     */
    public SettingsRest(final int port, final String layout) {
        this.port = port;
        this.layout = layout;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public void init(final RouterBuilder rbr) {
        rbr.operation("port")
            .handler(this::portRest)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("layout")
            .handler(this::layoutRest)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
    }

    /**
     * Send json with Artipie's port and status code OK_200.
     * @param context Request context
     */
    private void portRest(final RoutingContext context) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("port", this.port);
        context.response()
            .setStatusCode(HttpStatus.OK_200)
            .end(builder.build().toString());
    }

    /**
     * Send json with layout of repository and status code OK_200.
     * @param context Request context
     */
    private void layoutRest(final RoutingContext context) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("layout", this.layout);
        context.response()
            .setStatusCode(HttpStatus.OK_200)
            .end(builder.build().toString());
    }
}
