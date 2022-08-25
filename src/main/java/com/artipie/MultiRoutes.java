/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Example of mounting several openipi-yaml files.
 * @checkstyle MagicNumberCheck (500 line)
 * @checkstyle ProhibitPublicStaticMethods (500 line)
 * @since 1.0
 */
@SuppressWarnings(
    {"PMD.AvoidDuplicateLiterals", "PMD.SystemPrintln", "PMD.ProhibitPublicStaticMethods"})
public final class MultiRoutes extends AbstractVerticle {
    /**
     * Deploys {@link MultiRoutes} by vertx.
     * @param args Program arguments
     * @checkstyle JavadocStyleCheck (5 line)
     */
    public static void main(final String[] args) {
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MultiRoutes());
    }

    // @checkstyle DesignForExtensionCheck (5 line)
    @Override
    public void start() {
        final Router router = new MountBuilder()
            .mount(
                "/shop/*", "swagger-ui/yaml/pet.yaml", builder -> {
                    final JsonArray pets = new JsonArray();
                    pets.add(new JsonObject()
                        .put("id", 5)
                        .put("name", "Kitty")
                    );
                    builder.operation("listPets").handler(
                        ctx -> ctx
                            .response()
                            .setStatusCode(200)
                            .end(pets.encode())
                    ).failureHandler(errorHandler(500));
                    builder.operation("createPet").handler(
                        ctx -> {
                            final JsonObject json = ctx.body().asJsonObject();
                            pets.add(new JsonObject()
                                .put("id", json.getInteger("id"))
                                .put("name", json.getString("name"))
                            );
                            ctx
                                .response()
                                .setStatusCode(200)
                                .end();
                        }
                    ).failureHandler(errorHandler(405));
                    builder.operation("showPetById").handler(
                        ctx -> {
                            final int id = Integer.parseInt(ctx.pathParam("petId"));
                            final Object pet = pets.stream()
                                .filter(p -> id == ((JsonObject) p).getInteger("id"))
                                .findFirst()
                                .orElse(new JsonObject());
                            ctx
                                .response()
                                .setStatusCode(200)
                                .end(((JsonObject) pet).encode());
                        }
                    ).failureHandler(errorHandler(500));
                })
            .mount(
                "/department/*", "swagger-ui/yaml/user.yaml", builder -> {
                    final JsonArray users = new JsonArray();
                    users.add(new JsonObject()
                        .put("id", 1)
                        .put("name", "Bob")
                    );
                    builder.operation("listUsers").handler(
                        ctx -> ctx
                            .response()
                            .setStatusCode(200)
                            .end(users.encode())
                    ).failureHandler(errorHandler(500));
                    builder.operation("listUsers2").handler(
                        ctx -> ctx
                            .response()
                            .setStatusCode(200)
                            .end(users.encode())
                    ).failureHandler(errorHandler(500));
                }
            ).build();
        final String path = asPath("swagger-ui").toUri().getPath();
        router.route("/api/users/*").handler(StaticHandler.create(FileSystemAccess.ROOT, path)
            .setCachingEnabled(false)
            .setIndexPage("index-user.html")
        );
        router.route("/api/pet/*").handler(StaticHandler.create(FileSystemAccess.ROOT, path)
            .setCachingEnabled(false)
            .setIndexPage("index-pet.html")
        );
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .onComplete(s -> System.out.println("Server started"))
            .onFailure(s -> System.err.println("Cannot start server"));
    }

    /**
     * Returns status code with error message.
     * @param code Status code
     * @return RouterContext handle
     */
    private static Handler<RoutingContext> errorHandler(final int code) {
        return routingContext -> {
            routingContext
                .response()
                .setStatusMessage(routingContext.failure().getMessage())
                .setStatusCode(code)
                .end();
            System.err.println(routingContext.failure().getMessage());
        };
    }

    /**
     * Obtains resources from context loader.
     * @param name Resource name
     * @return File path
     */
    private static Path asPath(final String name) {
        try {
            return Paths.get(
                Objects.requireNonNull(
                    Thread.currentThread().getContextClassLoader().getResource(name)
                ).toURI()
            );
        } catch (final URISyntaxException ex) {
            throw new IllegalStateException("Failed to obtain recourse", ex);
        }
    }

    /**
     * Allows to mount several openapi specification sub routers and build main router
     * that includes sub routers.
     * @since 1.0
     */
    class MountBuilder {
        /**
         * Main router that mounts sub routers.
         */
        private final Router main = Router.router(vertx);

        /**
         * Builds sub router from openapi specification and mount it by path.
         * @param path Mounting path
         * @param yaml Openapi yaml file
         * @param fun Custom function to adjust sub router
         * @return Self reference
         */
        public MountBuilder mount(
            final String path, final String yaml,
            final Consumer<RouterBuilder> fun) {
            RouterBuilder.create(vertx, yaml)
                .toCompletionStage().toCompletableFuture()
                .thenAccept(
                    rb -> {
                        fun.accept(rb);
                        this.main.route(path).subRouter(rb.createRouter());
                    }
                );
            return this;
        }

        /**
         * Builds main router.
         * @return Main router
         */
        public Router build() {
            return this.main;
        }
    }
}
