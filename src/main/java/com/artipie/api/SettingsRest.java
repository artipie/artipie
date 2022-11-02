/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.settings.Settings;
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
     * Settings.
     */
    private final Settings settings;

    /**
     * Ctor.
     * @param settings Settings
     */
    public SettingsRest(final Settings settings) {
        this.settings = settings;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public void init(final RouterBuilder rbr) {
        rbr.operation("layout")
            .handler(this::layout)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
    }

    /**
     * Layout of repository.
     * @param context Request context
     */
    private void layout(final RoutingContext context) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("layout", this.settings.layout().toString());
        context.response()
            .setStatusCode(HttpStatus.OK_200)
            .end(builder.build().toString());
    }
}
