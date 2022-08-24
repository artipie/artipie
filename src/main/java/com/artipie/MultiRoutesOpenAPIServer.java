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

public class MultiRoutesOpenAPIServer extends AbstractVerticle {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MultiRoutesOpenAPIServer());
    }

    @Override
    public void start() {
        final Router router = new MultiYamlBuilder()
            .addYaml("/shop/*", "swagger-ui/yaml/pet.yaml", rb -> {
                final JsonArray pets = new JsonArray();
                pets.add(new JsonObject()
                    .put("id", 5)
                    .put("name", "Kitty"));

                rb.operation("listPets").handler(context -> context
                    .response()
                    .setStatusCode(200)
                    .end(pets.encode())
                ).failureHandler(errorHandler(500));

                rb.operation("createPet").handler(context -> {
                    final JsonObject json = context.body().asJsonObject();
                    pets.add(new JsonObject()
                        .put("id", json.getInteger("id"))
                        .put("name", json.getString("name"))
                    );
                    context
                        .response()
                        .setStatusCode(200)
                        .end();
                }).failureHandler(errorHandler(405));

                rb.operation("showPetById").handler(ctx -> {
                    final int petId = Integer.parseInt(ctx.pathParam("petId"));
                    final Object pet = pets.stream()
                        .filter(p -> petId == ((JsonObject) p).getInteger("id"))
                        .findFirst()
                        .orElse(new JsonObject());
                    ctx
                        .response()
                        .setStatusCode(200)
                        .end(((JsonObject) pet).encode());

                }).failureHandler(errorHandler(500));
            })
            .addYaml("/department/*", "swagger-ui/yaml/user.yaml", rb -> {
                final JsonArray users = new JsonArray();
                users.add(new JsonObject()
                    .put("id", 1)
                    .put("name", "Bob"));

                rb.operation("listUsers").handler(context -> context
                    .response()
                    .setStatusCode(200)
                    .end(users.encode())
                ).failureHandler(errorHandler(500));

                rb.operation("listUsers2").handler(context -> context
                    .response()
                    .setStatusCode(200)
                    .end(users.encode())
                ).failureHandler(errorHandler(500));
            })
            .build();

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

    class MultiYamlBuilder {
        final Router main = Router.router(vertx);

        public MultiYamlBuilder addYaml(String path, String yaml, Consumer<RouterBuilder> f) {
            RouterBuilder.create(vertx, yaml)
                .toCompletionStage().toCompletableFuture()
                .thenAccept(rb -> {
                    f.accept(rb);
                    main.route(path).subRouter(rb.createRouter());
                });
            return this;
        }

        public Router build() {
            return main;
        }
    }

    private Handler<RoutingContext> errorHandler(final int code) {
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
     *
     * @return File path
     */
    public static Path asPath(String name) {
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
