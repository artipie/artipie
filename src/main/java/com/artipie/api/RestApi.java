/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.misc.JavaResource;
import com.artipie.settings.RepoData;
import com.artipie.settings.cache.SettingsCaches;
import com.jcabi.log.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.util.Optional;

/**
 * Vert.x {@link io.vertx.core.Verticle} for exposing Rest API operations.
 * @since 0.26
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class RestApi extends AbstractVerticle {

    /**
     * Artipie setting cache.
     */
    private final SettingsCaches caches;

    /**
     * Artipie settings storage.
     */
    private final Storage storage;

    /**
     * Artipie layout.
     */
    private final String layout;

    /**
     * Application port.
     */
    private final int port;

    /**
     * Key to users credentials yaml file location.
     */
    private final Optional<Key> users;

    /**
     * Ctor.
     * @param caches Artipie settings caches
     * @param storage Artipie settings storage.
     * @param layout Artipie layout
     * @param port Port to start verticle on
     * @param users Key to users credentials yaml location
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public RestApi(final SettingsCaches caches, final Storage storage, final String layout,
        final int port, final Optional<Key> users) {
        this.caches = caches;
        this.storage = storage;
        this.layout = layout;
        this.port = port;
        this.users = users;
    }

    @Override
    public void start() throws Exception {
        RouterBuilder.create(this.vertx, String.format("swagger-ui/yaml/%s.yaml", this.layout))
            .compose(
                rrb -> RouterBuilder.create(this.vertx, "swagger-ui/yaml/users.yaml").onSuccess(
                    urb -> {
                        final BlockingStorage asto = new BlockingStorage(this.storage);
                        new RepositoryRest(
                            new ManageRepoSettings(asto),
                            new RepoData(this.storage),
                            this.layout
                        ).init(rrb);
                        new StorageAliasesRest(this.caches.storageConfig(), asto, this.layout)
                            .init(rrb);
                        if (this.users.isPresent()) {
                            new UsersRest(new ManageUsers(this.users.get(), asto)).init(urb);
                        } else {
                            Logger.warn(
                                this, "File credentials are not set, users API is not available"
                            );
                        }
                        final Router router = rrb.createRouter();
                        router.route("/*").subRouter(urb.createRouter());
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
                            .onComplete(res -> Logger.info(this, "Rest API started"))
                            .onFailure(err -> Logger.error(this, err.getMessage()));
                    }
                ).onFailure(Throwable::printStackTrace)
            );
    }
}
