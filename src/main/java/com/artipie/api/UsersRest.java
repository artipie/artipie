/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.settings.users.CrudUsers;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.util.Optional;
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
        rbr.operation("getUser")
            .handler(this::getUser)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
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

}
