/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.settings.Layout;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Rest API methods to manage storage aliases.
 * @since 0.27
 */
public final class StorageAliasesRest extends BaseRest {

    /**
     * Artipie settings storage.
     */
    private final BlockingStorage asto;

    /**
     * Artipie layout.
     */
    private final String layout;

    /**
     * Ctor.
     * @param asto Artipie settings storage
     * @param layout Artipie layout
     */
    public StorageAliasesRest(final BlockingStorage asto, final String layout) {
        this.asto = asto;
        this.layout = layout;
    }

    @Override
    public void init(final RouterBuilder rtrb) {
        if (new Layout.Flat().toString().equals(this.layout)) {
            rtrb.operation("getAliases")
                .handler(this::getAliases)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rtrb.operation("getRepoAliases")
                .handler(this::getRepoAliases)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        }
    }

    /**
     * Get repository aliases.
     * @param context Routing context
     */
    private void getAliases(final RoutingContext context) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        new ManageStorageAliases(this.asto).list().forEach(builder::add);
        context.response().setStatusCode(HttpStatus.OK_200).end(builder.build().toString());
    }

    /**
     * Get common artipie aliases.
     * @param context Routing context
     */
    private void getRepoAliases(final RoutingContext context) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        new ManageStorageAliases(
            new Key.From(context.pathParam(RepositoryName.RNAME)), this.asto
        ).list().forEach(builder::add);
        context.response().setStatusCode(HttpStatus.OK_200).end(
            builder.build().toString()
        );
    }
}
