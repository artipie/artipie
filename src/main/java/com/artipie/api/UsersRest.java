/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.api.perms.ApiUserPermission;
import com.artipie.asto.misc.Cleanable;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.security.policy.Policy;
import com.artipie.settings.ArtipieSecurity;
import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.settings.users.CrudUsers;
import com.jcabi.log.Logger;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.io.StringReader;
import java.security.PermissionCollection;
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
     * User name path param.
     */
    private static final String USER_NAME = "uname";

    /**
     * Update user permission.
     */
    private static final ApiUserPermission UPDATE =
        new ApiUserPermission(ApiUserPermission.UserAction.UPDATE);

    /**
     * Create user permission.
     */
    private static final ApiUserPermission CREATE =
        new ApiUserPermission(ApiUserPermission.UserAction.CREATE);

    /**
     * Crud users object.
     */
    private final CrudUsers users;

    /**
     * Artipie authenticated users cache.
     */
    private final Cleanable<String> ucache;

    /**
     * Artipie authenticated users cache.
     */
    private final Cleanable<String> pcache;

    /**
     * Artipie auth.
     */
    private final Authentication auth;

    /**
     * Artipie security policy.
     */
    private final Policy<?> policy;

    /**
     * Ctor.
     * @param users Crud users object
     * @param caches Artipie caches
     * @param security Artipie security
     */
    public UsersRest(final CrudUsers users, final ArtipieCaches caches,
        final ArtipieSecurity security) {
        this.users = users;
        this.ucache = caches.usersCache();
        this.pcache = caches.policyCache();
        this.auth = security.authentication();
        this.policy = security.policy();
    }

    @Override
    public void init(final RouterBuilder rbr) {
        rbr.operation("listAllUsers")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiUserPermission(ApiUserPermission.UserAction.READ)
                )
            )
            .handler(this::listAllUsers)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("getUser")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiUserPermission(ApiUserPermission.UserAction.READ)
                )
            )
            .handler(this::getUser)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("putUser")
            .handler(this::putUser)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("deleteUser")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiUserPermission(ApiUserPermission.UserAction.DELETE)
                )
            )
            .handler(this::deleteUser)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("alterPassword")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiUserPermission(ApiUserPermission.UserAction.CHANGE_PASSWORD)
                )
            )
            .handler(this::alterPassword)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("enable")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiUserPermission(ApiUserPermission.UserAction.ENABLE)
                )
            )
            .handler(this::enableUser)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("disable")
            .handler(
                new AuthzHandler(
                    this.policy, new ApiUserPermission(ApiUserPermission.UserAction.ENABLE)
                )
            )
            .handler(this::disableUser)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
    }

    /**
     * Removes user.
     * @param context Request context
     */
    private void deleteUser(final RoutingContext context) {
        final String uname = context.pathParam(UsersRest.USER_NAME);
        try {
            this.users.remove(uname);
        } catch (final IllegalStateException err) {
            Logger.error(this, err.getMessage());
            context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
            return;
        }
        this.ucache.invalidate(uname);
        this.pcache.invalidate(uname);
        context.response().setStatusCode(HttpStatus.OK_200).end();
    }

    /**
     * Removes user.
     * @param context Request context
     */
    private void enableUser(final RoutingContext context) {
        final String uname = context.pathParam(UsersRest.USER_NAME);
        try {
            this.users.enable(uname);
        } catch (final IllegalStateException err) {
            Logger.error(this, err.getMessage());
            context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
            return;
        }
        this.ucache.invalidate(uname);
        this.pcache.invalidate(uname);
        context.response().setStatusCode(HttpStatus.OK_200).end();
    }

    /**
     * Removes user.
     * @param context Request context
     */
    private void disableUser(final RoutingContext context) {
        final String uname = context.pathParam(UsersRest.USER_NAME);
        try {
            this.users.disable(uname);
        } catch (final IllegalStateException err) {
            Logger.error(this, err.getMessage());
            context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
            return;
        }
        this.ucache.invalidate(uname);
        this.pcache.invalidate(uname);
        context.response().setStatusCode(HttpStatus.OK_200).end();
    }

    /**
     * Create or replace existing user taking into account permissions of the
     * logged-in user.
     * @param context Request context
     */
    private void putUser(final RoutingContext context) {
        final String uname = context.pathParam(UsersRest.USER_NAME);
        final Optional<JsonObject> existing = this.users.get(uname);
        final PermissionCollection perms = this.policy.getPermissions(
            new AuthUser(
                context.user().principal().getString(AuthTokenRest.SUB),
                context.user().principal().getString(AuthTokenRest.CONTEXT)
            )
        );
        if (existing.isPresent() && perms.implies(UsersRest.UPDATE)
            || existing.isEmpty() && perms.implies(UsersRest.CREATE)) {
            this.users.addOrUpdate(
                Json.createReader(new StringReader(context.body().asString())).readObject(), uname
            );
            this.ucache.invalidate(uname);
            this.pcache.invalidate(uname);
            context.response().setStatusCode(HttpStatus.CREATED_201).end();
        } else {
            context.response().setStatusCode(HttpStatus.FORBIDDEN_403).end();
        }
    }

    /**
     * Get single user info.
     * @param context Request context
     */
    private void getUser(final RoutingContext context) {
        final Optional<JsonObject> usr = this.users.get(
            context.pathParam(UsersRest.USER_NAME)
        );
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
        final String uname = context.pathParam(UsersRest.USER_NAME);
        final JsonObject body = readJsonObject(context);
        final Optional<AuthUser> usr = this.auth.user(uname, body.getString("old_pass"));
        if (usr.isPresent()) {
            try {
                this.users.alterPassword(uname, body);
                context.response().setStatusCode(HttpStatus.OK_200).end();
                this.ucache.invalidate(uname);
            } catch (final IllegalStateException err) {
                Logger.error(this, err.getMessage());
                context.response().setStatusCode(HttpStatus.NOT_FOUND_404).end();
            }
        } else {
            context.response().setStatusCode(HttpStatus.UNAUTHORIZED_401).end();
        }
    }

}
