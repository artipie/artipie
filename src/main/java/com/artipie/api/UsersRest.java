/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.http.auth.Authentication;
import com.artipie.settings.cache.Cleanable;
import com.artipie.settings.users.CrudUsers;
import com.jcabi.log.Logger;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.io.StringReader;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.eclipse.jetty.http.HttpStatus;

/**
 * REST API methods to manage Artipie users.
 * @since 0.27
 */
public final class UsersRest extends BaseRest {

    /**
     * Crud users object.
     */
    private final CrudUsers users;

    /**
     * Artipie authenticated users cache.
     */
    private final Cleanable cache;

    /**
     * Artipie auth.
     */
    private final Authentication auth;

    /**
     * Ctor.
     * @param users Crud users object
     * @param cache Artipie authenticated users cache
     * @param auth Artipie authentication
     */
    public UsersRest(final CrudUsers users, final Cleanable cache, final Authentication auth) {
        this.users = users;
        this.cache = cache;
        this.auth = auth;
    }

    @Override
    public void init(final RouterBuilder rbr) {
        rbr.operation("listAllUsers")
            .handler(this::listAllUsers)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("getUser")
            .handler(this::getUser)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("putUser")
            .handler(this::putUser)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("deleteUser")
            .handler(this::deleteUser)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("alterPassword")
            .handler(this::alterPassword)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
    }

    /**
     * Removes user.
     * @param context Request context
     */
    private void deleteUser(final RoutingContext context) {
        try {
            this.users.remove(context.pathParam(RepositoryName.UNAME));
        } catch (final IllegalStateException err) {
            Logger.error(this, err.getMessage());
            context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
            return;
        }
        this.cache.invalidate();
        context.response().setStatusCode(HttpStatus.OK_200).end();
    }

    /**
     * Create or replace existing user.
     * @param context Request context
     */
    private void putUser(final RoutingContext context) {
        this.users.addOrUpdate(
            Json.createReader(new StringReader(context.body().asString())).readObject(),
            context.pathParam(RepositoryName.UNAME)
        );
        this.cache.invalidate();
        context.response().setStatusCode(HttpStatus.CREATED_201).end();
    }

    /**
     * Get single user info.
     * @param context Request context
     */
    private void getUser(final RoutingContext context) {
        final Optional<JsonObject> usr = this.users.get(context.pathParam(RepositoryName.UNAME));
        if (usr.isPresent()) {
            context.response().setStatusCode(HttpStatus.OK_200).end(usr.get().toString());
        } else {
            context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
        }
    }

    /**
     * List all users.
     * @param context Request context
     */
    private void listAllUsers(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(this.users.list().toString());
    }

    /**
     * Alter user password.
     * @param context Routing context
     */
    private void alterPassword(final RoutingContext context) {
        final String uname = context.pathParam(RepositoryName.UNAME);
        final JsonObject body = readJsonObject(context);
        final Optional<Authentication.User> usr =
            this.auth.user(uname, body.getString("old_pass"));
        if (usr.isPresent()) {
            try {
                this.users.alterPassword(uname, body);
                context.response().setStatusCode(HttpStatus.OK_200).end();
                this.cache.invalidate();
            } catch (final IllegalStateException err) {
                Logger.error(this, err.getMessage());
                context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
            }
        } else {
            context.response().setStatusCode(HttpStatus.UNAUTHORIZED_401).end();
        }
    }

}
