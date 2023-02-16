/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.api.ssl.KeyStore;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.auth.JwtTokens;
import com.artipie.http.auth.Authentication;
import com.artipie.settings.RepoData;
import com.artipie.settings.Settings;
import com.artipie.settings.cache.ArtipieCaches;
import com.jcabi.log.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.util.Arrays;
import java.util.Optional;

/**
 * Vert.x {@link io.vertx.core.Verticle} for exposing Rest API operations.
 * @since 0.26
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MemberNameCheck (500 lines)
 * @checkstyle ParameterNameCheck (500 lines)
 */
public final class RestApi extends AbstractVerticle {

    /**
     * The name of the security scheme (from the Open API description yaml).
     */
    private static final String SECURITY_SCHEME = "bearerAuth";

    /**
     * Artipie caches.
     */
    private final ArtipieCaches caches;

    /**
     * Artipie settings storage.
     */
    private final Storage configsStorage;

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
     * KeyStore.
     */
    private final Optional<KeyStore> keystore;

    /**
     * Jwt authentication provider.
     */
    private final JWTAuth jwt;

    /**
     * Primary ctor.
     * @param caches Artipie settings caches
     * @param configsStorage Artipie settings storage
     * @param layout Artipie layout
     * @param port Port to run API on
     * @param users Key to users credentials yaml file location
     * @param auth Artipie authentication
     * @param keystore KeyStore
     * @param jwt Jwt authentication provider
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public RestApi(
        final ArtipieCaches caches,
        final Storage configsStorage,
        final String layout,
        final int port, final Optional<Key> users,
        final Authentication auth,
        final Optional<KeyStore> keystore,
        final JWTAuth jwt
    ) {
        this.caches = caches;
        this.configsStorage = configsStorage;
        this.layout = layout;
        this.port = port;
        this.users = users;
        this.auth = auth;
        this.keystore = keystore;
        this.jwt = jwt;
    }

    /**
     * Ctor.
     * @param settings Artipie settings
     * @param port Port to start verticle on
     * @param jwt Jwt authentication provider
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public RestApi(final Settings settings, final int port, final JWTAuth jwt) {
        this(
            settings.caches(), settings.configStorage(), settings.layout().toString(),
            port, settings.credentialsKey(), settings.auth(), settings.keyStore(), jwt
        );
    }

    @Override
    public void start() throws Exception {
        RouterBuilder.create(this.vertx, String.format("swagger-ui/yaml/repo-%s.yaml", this.layout))
            .compose(
                repoRb -> RouterBuilder.create(this.vertx, "swagger-ui/yaml/users.yaml").compose(
                    userRb -> RouterBuilder.create(this.vertx, "swagger-ui/yaml/token-gen.yaml")
                        .compose(
                            //@checkstyle LineLengthCheck (1 line)
                            tokenRb -> RouterBuilder.create(this.vertx, "swagger-ui/yaml/settings.yaml")
                                .onSuccess(
                                    settingsRb -> {
                                        this.startServices(repoRb, userRb, tokenRb, settingsRb);
                                    }
                                ).onFailure(Throwable::printStackTrace)
                        )
                )
            );
    }

    /**
     * Start rest services.
     * @param repoRb Repository RouterBuilder
     * @param userRb User RouterBuilder
     * @param tokenRb Token RouterBuilder
     * @param settingsRb Settings RouterBuilder
     * @checkstyle ParameterNameCheck (4 lines)
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    private void startServices(final RouterBuilder repoRb, final RouterBuilder userRb,
        final RouterBuilder tokenRb, final RouterBuilder settingsRb) {
        this.addJwtAuth(tokenRb, repoRb, userRb, settingsRb);
        final BlockingStorage asto = new BlockingStorage(this.configsStorage);
        new RepositoryRest(
            new ManageRepoSettings(asto),
            new RepoData(this.configsStorage, this.caches.storagesCache()), this.layout
        ).init(repoRb);
        new StorageAliasesRest(this.caches.storagesCache(), asto, this.layout)
            .init(repoRb);
        if (this.users.isPresent()) {
            new UsersRest(
                new ManageUsers(this.users.get(), asto),
                this.caches.usersCache(), this.auth
            ).init(userRb);
        } else {
            Logger.warn(this, "File credentials are not set, users API is not available");
        }
        new SettingsRest(this.port, this.layout).init(settingsRb);
        final Router router = repoRb.createRouter();
        router.route("/*").subRouter(userRb.createRouter());
        router.route("/*").subRouter(tokenRb.createRouter());
        router.route("/*").subRouter(settingsRb.createRouter());
        router.route("/api/*").handler(
            StaticHandler.create("swagger-ui")
                .setIndexPage(String.format("index-%s.html", this.layout))
        );
        final HttpServer server;
        final String schema;
        if (this.keystore.isPresent() && this.keystore.get().enabled()) {
            server = vertx.createHttpServer(
                this.keystore.get().secureOptions(this.vertx, this.configsStorage)
            );
            schema = "https";
        } else {
            server = this.vertx.createHttpServer();
            schema = "http";
        }
        server.requestHandler(router)
            .listen(this.port)
            //@checkstyle LineLengthCheck (1 line)
            .onComplete(res -> Logger.info(this, String.format("Rest API started on port %d, swagger is available on %s://localhost:%d/api/index-%s.html", this.port, schema, this.port, this.layout)))
            .onFailure(err -> Logger.error(this, err.getMessage()));
    }

    /**
     * Create and add all JWT-auth related settings:
     *  - initialize rest method to issue JWT tokens;
     *  - add security handlers to all REST API requests.
     * @param token Auth tokens generate API router builder
     * @param builders Router builders to add token auth to
     */
    private void addJwtAuth(final RouterBuilder token, final RouterBuilder... builders) {
        new AuthTokenRest(new JwtTokens(this.jwt), this.auth).init(token);
        Arrays.stream(builders).forEach(
            item -> item.securityHandler(RestApi.SECURITY_SCHEME, JWTAuthHandler.create(this.jwt))
        );
    }
}
