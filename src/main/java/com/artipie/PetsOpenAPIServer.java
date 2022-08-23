package com.artipie;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;

public class PetsOpenAPIServer extends AbstractVerticle {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new PetsOpenAPIServer());
    }

    private JsonArray pets = new JsonArray();

    @Override
    public void start() {
        RouterBuilder.create(vertx, "petstore.yaml")
            .onSuccess(rb -> {
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
                final HttpServer server = vertx.createHttpServer();
                server.requestHandler(router)
                    .listen(8080)
                    .onComplete(s -> {
                        System.out.println("Server started");
                    })
                    .onFailure(s -> System.err.println("Cannot start server"));
            }).onFailure(Throwable::printStackTrace);
    }

    private void listPets(RoutingContext routingContext) {
        routingContext
            .response()
            .setStatusCode(200)
            .end(pets.encode());
    }

    private void createPet(RoutingContext routingContext) {
        final JsonObject json = routingContext.body().asJsonObject();
        pets.add(new JsonObject()
            .put("id", json.getInteger("id"))
            .put("name", json.getString("name"))
        );
        routingContext
            .response()
            .setStatusCode(200)
            .end();
    }

    private void showPetById(RoutingContext routingContext) {
        final int petId = Integer.parseInt(routingContext.pathParam("petId"));
        final Object pet = pets.stream()
            .filter(p -> petId == ((JsonObject) p).getInteger("id"))
            .findFirst()
            .orElse(new JsonObject());
        routingContext
            .response()
            .setStatusCode(200)
            .end(((JsonObject)pet).encode());
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
}
