/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for PetsOpenapi.
 * @checkstyle MagicNumberCheck (500 line)
 * @checkstyle SystemPrintln (500 line)
 * @checkstyle AvoidDuplicateLiterals (500 line)
 * @since 1.0
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.SystemPrintln"})
@ExtendWith(VertxExtension.class)
class PetsopenapiTest {
    @BeforeEach
    void prepare(final Vertx vertx, final VertxTestContext ctx)
        throws ExecutionException, InterruptedException {
        vertx.deployVerticle(new PetsOpenapi())
            .onSuccess(ok -> ctx.completeNow())
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture().get();
        this.waitHttpServerUp(vertx);
    }

    @Test
    void postPets(final Vertx vertx, final VertxTestContext ctx) {
        WebClient.create(vertx)
            .post(8080, "localhost", "/pets")
            .sendJsonObject(
                new JsonObject()
                    .put("id", 1)
                    .put("name", "Tom")
            )
            .onSuccess(
                res -> {
                    Assertions.assertEquals(200, res.statusCode());
                    ctx.completeNow();
                }
            )
            .onFailure(ctx::failNow);
    }

    @Test
    void getPets(final Vertx vertx, final VertxTestContext ctx) {
        final WebClient client = WebClient.create(vertx);
        client
            .post(8080, "localhost", "/pets")
            .sendJsonObject(
                new JsonObject()
                    .put("id", 1)
                    .put("name", "Tom")
            )
            .onSuccess(
                res -> Assertions.assertEquals(200, res.statusCode())
            )
            .onFailure(ctx::failNow);
        client
            .get(8080, "localhost", "/pets")
            .send()
            .onSuccess(
                res -> {
                    Assertions.assertEquals(200, res.statusCode());
                    final JsonArray pets = res.bodyAsJsonArray();
                    Assertions.assertEquals(1, pets.size());
                    Assertions.assertEquals(1, pets.getJsonObject(0).getInteger("id"));
                    Assertions.assertEquals("Tom", pets.getJsonObject(0).getString("name"));
                    ctx.completeNow();
                }
            )
            .onFailure(ctx::failNow);
    }

    @Test
    void getPetById(final Vertx vertx, final VertxTestContext ctx) {
        final WebClient client = WebClient.create(vertx);
        client
            .post(8080, "localhost", "/pets")
            .sendJsonObject(
                new JsonObject()
                    .put("id", 1)
                    .put("name", "Tom")
            )
            .onSuccess(
                res -> {
                    Assertions.assertEquals(200, res.statusCode());
                }
            )
            .onFailure(ctx::failNow);
        client
            .get(8080, "localhost", "/pets/1")
            .send()
            .onSuccess(
                res -> {
                    Assertions.assertEquals(200, res.statusCode());
                    final JsonObject pet = res.bodyAsJsonObject();
                    Assertions.assertEquals(1, pet.getInteger("id"));
                    Assertions.assertEquals("Tom", pet.getString("name"));
                    ctx.completeNow();
                }
            )
            .onFailure(ctx::failNow);
    }

    private void waitHttpServerUp(final Vertx vertx) throws InterruptedException {
        final NetClient client = vertx.createNetClient();
        final AtomicReference<Boolean> available = new AtomicReference<>(false);
        int retry = 10;
        while (!available.get() && retry > 0) {
            client.connect(
                8080, "localhost",
                ar -> {
                    if (ar.succeeded()) {
                        available.set(true);
                    }
                }
            );
            if (!available.get()) {
                Thread.sleep(500);
                retry -= 1;
            }
        }
        if (!available.get()) {
            Assertions.fail();
        }
    }
}

