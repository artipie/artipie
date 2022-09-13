/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.settings.users.CrudUsers;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
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
     * Ctor.
     * @param users Crud users object
     */
    public UsersRest(final CrudUsers users) {
        this.users = users;
    }

    @Override
    public void init(final RouterBuilder rbr) {
        rbr.operation("listAllUsers")
            .handler(this::listAllUsers)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
    }

    /**
     * List all users.
     * @param context Request context
     */
    private void listAllUsers(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200)
            .end(this.users.list().toString());
    }

}
