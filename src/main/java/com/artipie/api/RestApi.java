/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.http.auth.Authentication;
import com.artipie.misc.JavaResource;
import com.artipie.settings.RepoData;
import com.artipie.settings.cache.SettingsCaches;
import com.jcabi.log.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.JWTAuthHandler;
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
     * The name of the security scheme (from the Open API description yaml).
     */
    private static final String SECURITY_SCHEME = "bearerAuth";

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
     * Artipie authentication.
     */
    private final Authentication auth;

    /**
     * Ctor.
     * @param caches Artipie settings caches
     * @param storage Artipie settings storage.
     * @param layout Artipie layout
     * @param port Port to start verticle on
     * @param users Key to users credentials yaml location
     * @param auth Artipie authentication
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public RestApi(final SettingsCaches caches, final Storage storage, final String layout,
        final int port, final Optional<Key> users, final Authentication auth) {
        this.caches = caches;
        this.storage = storage;
        this.layout = layout;
        this.port = port;
        this.users = users;
        this.auth = auth;
    }

    @Override
    public void start() throws Exception {
        RouterBuilder.create(this.vertx, String.format("swagger-ui/yaml/repo-%s.yaml", this.layout))
            .compose(
                repoRb -> RouterBuilder.create(this.vertx, "swagger-ui/yaml/users.yaml").compose(
                    //@checkstyle LineLengthCheck (1 line)
                    userRb -> RouterBuilder.create(this.vertx, "swagger-ui/yaml/token-gen.yaml").onSuccess(
                        tokenRb -> {
                            this.addJwtAuth(repoRb, userRb, tokenRb);
                            final BlockingStorage asto = new BlockingStorage(this.storage);
                            new RepositoryRest(
                                new ManageRepoSettings(asto),
                                new RepoData(this.storage), this.layout
                            ).init(repoRb);
                            new StorageAliasesRest(this.caches.storageConfig(), asto, this.layout)
                                .init(repoRb);
                            if (this.users.isPresent()) {
                                new UsersRest(
                                    new ManageUsers(this.users.get(), asto), this.caches.auth()
                                ).init(userRb);
                            } else {
                                Logger.warn(
                                    this, "File credentials are not set, users API is not available"
                                );
                            }
                            final Router router = repoRb.createRouter();
                            router.route("/*").subRouter(userRb.createRouter());
                            router.route("/*").subRouter(tokenRb.createRouter());
                            router.route("/api/*").handler(
                                StaticHandler.create(
                                    FileSystemAccess.ROOT,
                                    new JavaResource("swagger-ui").uri().getPath()
                                ).setIndexPage(String.format("index-%s.html", this.layout))
                            );
                            final HttpServer server = this.vertx.createHttpServer();
                            server.requestHandler(router)
                                .listen(this.port)
                                .onComplete(res -> Logger.info(this, "Rest API started"))
                                .onFailure(err -> Logger.error(this, err.getMessage()));
                        }
                    ).onFailure(Throwable::printStackTrace)
                )
            );
    }

    /**
     * Create and add all JWT-auth related settings:
     *  - initialize rest method to issue JWT tokens;
     *  - add security handlers to all REST API requests.
     * @param repo Repository API router builder
     * @param user Users API router builder
     * @param token Auth tokens generate API router builder
     */
    private void addJwtAuth(final RouterBuilder repo, final RouterBuilder user,
        final RouterBuilder token) {
        final JWTAuth jwt = JWTAuth.create(
            this.vertx, new JWTAuthOptions().addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("HS256").setBuffer("some secret")
            )
        );
        new AuthTokenRest(jwt, this.caches.auth(), this.auth).init(token);
        repo.securityHandler(RestApi.SECURITY_SCHEME, JWTAuthHandler.create(jwt));
        user.securityHandler(RestApi.SECURITY_SCHEME, JWTAuthHandler.create(jwt));
    }
}
