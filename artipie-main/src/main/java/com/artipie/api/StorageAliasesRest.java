/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.api.perms.ApiAliasPermission;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.security.policy.Policy;
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
     * Artipie policy.
     */
    private final Policy<?> policy;

    /**
     * Ctor.
     * @param caches Artipie settings caches
     * @param asto Artipie settings storage
     * @param policy Artipie policy
     */
    public StorageAliasesRest(final StoragesCache caches, final BlockingStorage asto,
        final Policy<?> policy) {
        this.caches = caches;
        this.asto = asto;
        this.policy = policy;
    }

    @Override
    public void init(final RouterBuilder rtrb) {
        rtrb.operation("addRepoAlias")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiAliasPermission(ApiAliasPermission.AliasAction.READ)
                )
            )
            .handler(this::addRepoAlias)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("getRepoAliases")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiAliasPermission(ApiAliasPermission.AliasAction.READ)
                )
            )
            .handler(this::getRepoAliases)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("deleteRepoAlias")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiAliasPermission(ApiAliasPermission.AliasAction.DELETE)
                )
            )
            .handler(this::deleteRepoAlias)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("getAliases")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiAliasPermission(ApiAliasPermission.AliasAction.READ)
                )
            )
            .handler(this::getAliases)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("addAlias")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiAliasPermission(ApiAliasPermission.AliasAction.CREATE)
                )
            )
            .handler(this::addAlias)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rtrb.operation("deleteAlias")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiAliasPermission(ApiAliasPermission.AliasAction.DELETE)
                )
            )
            .handler(this::deleteAlias)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
    }

    /**
     * Delete repository alias.
     * @param context Routing context
     */
    private void deleteRepoAlias(final RoutingContext context) {
        this.delete(
            context, Optional.of(
                new Key.From(new RepositoryName.FromRequest(context).toString())
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
     * Add repository alias.
     * @param context Routing context
     */
    private void addRepoAlias(final RoutingContext context) {
        new ManageStorageAliases(
            new Key.From(new RepositoryName.FromRequest(context).toString()), this.asto
        ).add(
            context.pathParam(StorageAliasesRest.ANAME),
            StorageAliasesRest.jsonFromRequest(context)
        );
        this.caches.invalidateAll();
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
        this.caches.invalidateAll();
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
                    new Key.From(new RepositoryName.FromRequest(context).toString())
                )
            )
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
            this.caches.invalidateAll();
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
