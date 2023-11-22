/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
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
     * Artipie port.
     */
    private final int port;

    /**
     * Ctor.
     * @param port Artipie port
     */
    public SettingsRest(final int port) {
        this.port = port;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public void init(final RouterBuilder rbr) {
        rbr.operation("port")
            .handler(this::portRest)
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
}
