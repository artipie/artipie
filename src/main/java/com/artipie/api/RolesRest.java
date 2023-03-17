/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.misc.Cleanable;
import com.artipie.settings.users.CrudRoles;
import com.jcabi.log.Logger;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.io.StringReader;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.eclipse.jetty.http.HttpStatus;

/**
 * REST API methods to manage Artipie roles.
 * @since 0.27
 */
public final class RolesRest extends BaseRest {

    /**
     * Role name path param.
     */
    private static final String ROLE_NAME = "role";

    /**
     * Crud roles object.
     */
    private final CrudRoles roles;

    /**
     * Artipie policy cache.
     */
    private final Cleanable<String> cache;

    /**
     * Ctor.
     * @param roles Crud roles object
     * @param cache Artipie authenticated roles cache
     */
    public RolesRest(final CrudRoles roles, final Cleanable<String> cache) {
        this.roles = roles;
        this.cache = cache;
    }

    @Override
    public void init(final RouterBuilder rbr) {
        rbr.operation("listAllRoles")
            .handler(this::listAllRoles)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("getRole")
            .handler(this::getRole)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("putRole")
            .handler(this::putRole)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("deleteRole")
            .handler(this::deleteRole)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("enable")
            .handler(this::enableRole)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("disable")
            .handler(this::disableRole)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
    }

    /**
     * Removes user.
     * @param context Request context
     */
    private void deleteRole(final RoutingContext context) {
        final String uname = context.pathParam(RolesRest.ROLE_NAME);
        try {
            this.roles.remove(uname);
        } catch (final IllegalStateException err) {
            Logger.error(this, err.getMessage());
            context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
            return;
        }
        this.cache.invalidate(uname);
        context.response().setStatusCode(HttpStatus.OK_200).end();
    }

    /**
     * Removes user.
     * @param context Request context
     */
    private void enableRole(final RoutingContext context) {
        final String uname = context.pathParam(RolesRest.ROLE_NAME);
        try {
            this.roles.enable(uname);
        } catch (final IllegalStateException err) {
            Logger.error(this, err.getMessage());
            context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
            return;
        }
        this.cache.invalidate(uname);
        context.response().setStatusCode(HttpStatus.OK_200).end();
    }

    /**
     * Removes user.
     * @param context Request context
     */
    private void disableRole(final RoutingContext context) {
        final String uname = context.pathParam(RolesRest.ROLE_NAME);
        try {
            this.roles.disable(uname);
        } catch (final IllegalStateException err) {
            Logger.error(this, err.getMessage());
            context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
            return;
        }
        this.cache.invalidate(uname);
        context.response().setStatusCode(HttpStatus.OK_200).end();
    }

    /**
     * Create or replace existing user.
     * @param context Request context
     */
    private void putRole(final RoutingContext context) {
        final String uname = context.pathParam(RolesRest.ROLE_NAME);
        this.roles.addOrUpdate(
            Json.createReader(new StringReader(context.body().asString())).readObject(),
            uname
        );
        this.cache.invalidate(uname);
        context.response().setStatusCode(HttpStatus.CREATED_201).end();
    }

    /**
     * Get single user info.
     * @param context Request context
     */
    private void getRole(final RoutingContext context) {
        final Optional<JsonObject> usr = this.roles.get(
            context.pathParam(RolesRest.ROLE_NAME)
        );
        if (usr.isPresent()) {
            context.response().setStatusCode(HttpStatus.OK_200).end(usr.get().toString());
        } else {
            context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
        }
    }

    /**
     * List all roles.
     * @param context Request context
     */
    private void listAllRoles(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(this.roles.list().toString());
    }

}
