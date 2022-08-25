/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.misc.JavaResource;
import com.artipie.settings.repo.CrudRepoSettings;
import com.jcabi.log.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Vert.x {@link io.vertx.core.Verticle} for repositories settings CRUD
 * (create/read/update/delete) operations.
 * @since 0.26
 */
public final class RepositoryVerticle extends AbstractVerticle {

    /**
     * Username path parameter name.
     */
    private static final String UNAME = "uname";

    /**
     * Repository settings create/read/update/delete.
     */
    private final CrudRepoSettings crs;

    /**
     * Artipie layout.
     */
    private final String layout;

    /**
     * Application port.
     */
    private final int port;

    /**
     * Ctor.
     * @param crs Repository settings create/read/update/delete
     * @param layout Artipie layout
     * @param port Port to start verticle on
     */
    public RepositoryVerticle(final CrudRepoSettings crs, final String layout, final int port) {
        this.crs = crs;
        this.layout = layout;
        this.port = port;
    }

    @Override
    public void start() throws Exception {
        RouterBuilder.create(this.vertx, "swagger-ui/yaml/repository.yaml")
            .onSuccess(
                rb -> {
                    rb.operation("listAll")
                        .handler(this::listAll)
                        .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
                    rb.operation("list")
                        .handler(this::listUserRepos)
                        .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
                    rb.operation("getRepo")
                        .handler(this::getRepo)
                        .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
                    final Router router = rb.createRouter();
                    router.route("/api/*").handler(
                        StaticHandler.create(
                            FileSystemAccess.ROOT,
                            new JavaResource("swagger-ui").uri().getPath()
                        )
                    );
                    final HttpServer server = this.vertx.createHttpServer();
                    server.requestHandler(router)
                        .listen(this.port)
                        .onComplete(res -> Logger.info(this, "Repositories API started"))
                        .onFailure(err -> Logger.error(this, err.getMessage()));
                }
            ).onFailure(Throwable::printStackTrace);
    }

    /**
     * Get repository settings json.
     * @param context Routing context
     */
    private void getRepo(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
            this.crs.value(
                this.name(context.pathParam("rname"), context.pathParam(RepositoryVerticle.UNAME))
            ).toString()
        );
    }

    /**
     * List all existing repositories.
     * @param context Routing context
     */
    private void listAll(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
            JsonArray.of(this.crs.listAll().toArray()).encode()
        );
    }

    /**
     * List all existing repositories.
     * @param context Routing context
     */
    private void listUserRepos(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
            JsonArray.of(this.crs.list(context.pathParam(RepositoryVerticle.UNAME))).encode()
        );
    }

    /**
     * Handle error.
     * @param code Error code
     * @return Error handler
     */
    private Handler<RoutingContext> errorHandler(final int code) {
        return context -> {
            context.response()
                .setStatusMessage(context.failure().getMessage())
                .setStatusCode(code)
                .end();
            Logger.error(this, context.failure().getMessage());
        };
    }

    /**
     * Returns repository name combined from username and repo name without yaml extension.
     * @param rname Repository name
     * @param uname User name
     * @return String name
     */
    private String name(final String rname, final String uname) {
        String res = rname;
        if (this.layout.equals("org")) {
            res = String.format("%s/%s", uname, rname);
        }
        return res;
    }
}
