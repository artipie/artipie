/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
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
     * Ctor.
     * @param asto Artipie settings storage
     */
    public StorageAliasesRest(final BlockingStorage asto) {
        this.asto = asto;
    }

    @Override
    public void init(final RouterBuilder rtrb) {
        rtrb.operation("getAliases")
            .handler(this::getAliases)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("getRepoAliases")
            .handler(this::getRepoAliases)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
    }

    /**
     * Get repository aliases.
     * @param context Routing context
     */
    private void getAliases(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
            JsonArray.of(
                new ManageStorageAliases(this.asto).list().stream()
                    .map(item -> new JsonObject(item.toString())).toArray()
            ).encode()
        );
    }

    /**
     * Get common artipie aliases.
     * @param context Routing context
     */
    private void getRepoAliases(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
            JsonArray.of(
                new ManageStorageAliases(
                    new Key.From(context.pathParam(RepositoryName.RNAME)), this.asto
                ).list().stream().map(item -> new JsonObject(item.toString())).toArray()
            ).encode()
        );
    }
}
