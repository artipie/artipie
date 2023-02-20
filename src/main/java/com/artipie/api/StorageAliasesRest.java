/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.settings.Layout;
import com.artipie.settings.cache.StoragesCache;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.io.StringReader;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Rest API methods to manage storage aliases.
 * @since 0.27
 */
public final class StorageAliasesRest extends BaseRest {

    /**
     * Alias name path parameter.
     */
    private static final String ANAME = "aname";

    /**
     * Artipie setting storage cache.
     */
    private final StoragesCache caches;

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
     * @param caches Artipie settings caches
     * @param asto Artipie settings storage
     * @param layout Artipie layout
     */
    public StorageAliasesRest(final StoragesCache caches, final BlockingStorage asto,
        final String layout) {
        this.caches = caches;
        this.asto = asto;
        this.layout = layout;
    }

    @Override
    public void init(final RouterBuilder rtrb) {
        rtrb.operation("addRepoAlias")
            .handler(this::addRepoAlias)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("getRepoAliases")
            .handler(this::getRepoAliases)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("deleteRepoAlias")
            .handler(this::deleteRepoAlias)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("getAliases")
            .handler(this::getAliases)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("addAlias")
            .handler(this::addAlias)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("deleteAlias")
            .handler(this::deleteAlias)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        if (new Layout.Org().toString().equals(this.layout)) {
            rtrb.operation("getUserAliases")
                .handler(this::getUserAliases)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rtrb.operation("addUserAlias")
                .handler(this::addUserAlias)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rtrb.operation("deleteUserAlias")
                .handler(this::deleteUserAlias)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        }
    }

    /**
     * Delete repository alias.
     * @param context Routing context
     */
    private void deleteRepoAlias(final RoutingContext context) {
        this.delete(
            context, Optional.of(
                new Key.From(new RepositoryName.FromRequest(context, this.layout).toString())
            )
        );
    }

    /**
     * Delete common Artipie alias.
     * @param context Routing context
     */
    private void deleteAlias(final RoutingContext context) {
        this.delete(context, Optional.empty());
    }

    /**
     * Delete user alias.
     * @param context Routing context
     */
    private void deleteUserAlias(final RoutingContext context) {
        this.delete(context, Optional.of(new Key.From(context.pathParam(RepositoryName.UNAME))));
    }

    /**
     * Add repository alias.
     * @param context Routing context
     */
    private void addRepoAlias(final RoutingContext context) {
        new ManageStorageAliases(
            new Key.From(new RepositoryName.FromRequest(context, this.layout).toString()), this.asto
        ).add(
            context.pathParam(StorageAliasesRest.ANAME),
            StorageAliasesRest.jsonFromRequest(context)
        );
        this.caches.invalidate();
        context.response().setStatusCode(HttpStatus.CREATED_201).end();
    }

    /**
     * Add common Artipie alias.
     * @param context Routing context
     */
    private void addAlias(final RoutingContext context) {
        new ManageStorageAliases(this.asto).add(
            context.pathParam(StorageAliasesRest.ANAME),
            StorageAliasesRest.jsonFromRequest(context)
        );
        this.caches.invalidate();
        context.response().setStatusCode(HttpStatus.CREATED_201).end();
    }

    /**
     * Add repository alias.
     * @param context Routing context
     */
    private void addUserAlias(final RoutingContext context) {
        new ManageStorageAliases(
            new Key.From(context.pathParam(RepositoryName.UNAME)), this.asto
        ).add(
            context.pathParam(StorageAliasesRest.ANAME),
            StorageAliasesRest.jsonFromRequest(context)
        );
        this.caches.invalidate();
        context.response().setStatusCode(HttpStatus.CREATED_201).end();
    }

    /**
     * Get common artipie aliases.
     * @param context Routing context
     */
    private void getAliases(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200)
            .end(this.aliases(Optional.empty()));
    }

    /**
     * Get repository aliases.
     * @param context Routing context
     */
    private void getRepoAliases(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
            this.aliases(
                Optional.of(
                    new Key.From(new RepositoryName.FromRequest(context, this.layout).toString())
                )
            )
        );
    }

    /**
     * Get user aliases.
     * @param context Routing context
     */
    private void getUserAliases(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
            this.aliases(Optional.of(new Key.From(context.pathParam(RepositoryName.UNAME))))
        );
    }

    /**
     * Get aliases as json array string.
     * @param key Aliases key
     * @return Json array string
     */
    private String aliases(final Optional<Key> key) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        new ManageStorageAliases(key, this.asto).list().forEach(builder::add);
        return builder.build().toString();
    }

    /**
     * Delete alias.
     * @param context Request context
     * @param key Aliases settings key, empty for common Artipie aliases
     */
    private void delete(final RoutingContext context, final Optional<Key> key) {
        try {
            new ManageStorageAliases(key, this.asto)
                .remove(context.pathParam(StorageAliasesRest.ANAME));
            this.caches.invalidate();
            context.response().setStatusCode(HttpStatus.OK_200).end();
        } catch (final IllegalStateException err) {
            context.response().setStatusCode(HttpStatus.NOT_FOUND_404)
                .end(err.getMessage());
        }
    }

    /**
     * Read json object from request.
     * @param context Request context
     * @return Javax json object
     */
    private static JsonObject jsonFromRequest(final RoutingContext context) {
        return Json.createReader(new StringReader(context.body().asString())).readObject();
    }
}
