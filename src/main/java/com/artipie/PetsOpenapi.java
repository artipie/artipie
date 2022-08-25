/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
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

/**
 * Example of openapi rest service.
 * @checkstyle MagicNumberCheck (500 line)
 * @checkstyle ProhibitPublicStaticMethods (500 line)
 * @since 1.0
 */
@SuppressWarnings(
    {"PMD.AvoidDuplicateLiterals", "PMD.SystemPrintln", "PMD.ProhibitPublicStaticMethods"})
public final class PetsOpenapi extends AbstractVerticle {
    /**
     * Pets-array.
     */
    private final JsonArray pets = new JsonArray();

    /**
     * Deploys {@link PetsOpenapi} by vertx.
     * @param args Program arguments
     * @checkstyle JavadocStyleCheck (5 line)
     */
    public static void main(final String[] args) {
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new PetsOpenapi());
    }

    // @checkstyle DesignForExtensionCheck (5 line)
    @Override
    public void start() {
        RouterBuilder.create(vertx, "swagger-ui/petstore.yaml")
            .onSuccess(
                rb -> {
                    rb.operation("listPets")
                        .handler(this::listPets)
                        .failureHandler(errorHandler(500));
                    rb.operation("createPet")
                        .handler(this::createPet)
                        .failureHandler(errorHandler(405));
                    rb.operation("showPetById")
                        .handler(this::showPetById)
                        .failureHandler(errorHandler(500));
                    final Router router = rb.createRouter();
                    router.route("/api/*").handler(
                        StaticHandler.create(
                            FileSystemAccess.ROOT, asPath("swagger-ui").toUri().getPath()
                        )
                    );
                    final HttpServer server = vertx.createHttpServer();
                    server.requestHandler(router)
                        .listen(8080)
                        .onComplete(
                            s -> {
                                System.out.println("Server started");
                            }
                        ).onFailure(s -> System.err.println("Cannot start server"));
                }
            ).onFailure(Throwable::printStackTrace);
    }

    /**
     * List of pets rest-service implementation.
     * @param ctx Routing context
     */
    private void listPets(final RoutingContext ctx) {
        ctx
            .response()
            .setStatusCode(200)
            .end(this.pets.encode());
    }

    /**
     * Create pet rest-service implementation.
     * @param ctx Routing context
     */
    private void createPet(final RoutingContext ctx) {
        final JsonObject json = ctx.body().asJsonObject();
        this.pets.add(new JsonObject()
            .put("id", json.getInteger("id"))
            .put("name", json.getString("name"))
        );
        ctx
            .response()
            .setStatusCode(200)
            .end();
    }

    /**
     * Finds pet by id rest-service implementation.
     * @param ctx Routing context
     */
    private void showPetById(final RoutingContext ctx) {
        final int id = Integer.parseInt(ctx.pathParam("petId"));
        final Object pet = this.pets.stream()
            .filter(p -> id == ((JsonObject) p).getInteger("id"))
            .findFirst()
            .orElse(new JsonObject());
        ctx
            .response()
            .setStatusCode(200)
            .end(((JsonObject) pet).encode());
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
}
