/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.misc.JavaResource;
import com.artipie.settings.repo.CrudRepoSettings;
import com.jcabi.log.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;

/**
 * Vert.x {@link io.vertx.core.Verticle} for exposing Rest API operations.
 * @since 0.26
 */
public final class RestApi extends AbstractVerticle {
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
    public RestApi(final CrudRepoSettings crs, final String layout, final int port) {
        this.crs = crs;
        this.layout = layout;
        this.port = port;
    }

    @Override
    public void start() throws Exception {
        RouterBuilder.create(this.vertx, String.format("swagger-ui/yaml/%s.yaml", this.layout))
            .onSuccess(
                rb -> {
                    new RepositoryRest(this.crs, this.layout).init(rb);
                    final Router router = rb.createRouter();
                    router.route("/api/*")
                        .handler(
                            StaticHandler
                                .create(
                                    FileSystemAccess.ROOT,
                                    new JavaResource("swagger-ui").uri().getPath()
                                )
                                .setIndexPage(String.format("index-%s.html", this.layout))
                        );
                    final HttpServer server = this.vertx.createHttpServer();
                    server.requestHandler(router)
                        .listen(this.port)
                        .onComplete(res -> Logger.info(this, "Repositories API started"))
                        .onFailure(err -> Logger.error(this, err.getMessage()));
                }
            ).onFailure(Throwable::printStackTrace);
    }
}
