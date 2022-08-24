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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class PetsOpenAPIServerTest {
    @BeforeEach
    void prepare(Vertx vertx, VertxTestContext testContext) throws ExecutionException, InterruptedException {
        vertx.deployVerticle(new PetsOpenAPIServer())
            .onSuccess(ok -> testContext.completeNow())
            .onFailure(testContext::failNow)
            .toCompletionStage().toCompletableFuture().get();
        waitHttpServerUp(vertx);
    }

    private void waitHttpServerUp(final Vertx vertx) throws InterruptedException {
        final NetClient tcpClient = vertx.createNetClient();
        final AtomicReference<Boolean> up = new AtomicReference<>(false);
        int retry = 10;
        while (!up.get() && retry > 0) {
            tcpClient.connect(8080, "localhost", ar -> {
                if (ar.succeeded()) {
                    up.set(true);
                }
            });
            if (!up.get()) {
                Thread.sleep(500);
                retry -= 1;
            }
        }
        if (!up.get()) {
            fail();
        }
    }

    @Test
    void postPets(final Vertx vertx, final VertxTestContext testContext) {
        WebClient.create(vertx)
            .post(8080, "localhost", "/pets")
            .sendJsonObject(
                new JsonObject()
                    .put("id", 1)
                    .put("name", "Tom"))
            .onSuccess(res -> {
                assertEquals(200, res.statusCode());
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }

    @Test
    void getPets(final Vertx vertx, final VertxTestContext testContext) {
        final WebClient client = WebClient.create(vertx);
        client
            .post(8080, "localhost", "/pets")
            .sendJsonObject(
                new JsonObject()
                    .put("id", 1)
                    .put("name", "Tom"))
            .onSuccess(res -> {
                assertEquals(200, res.statusCode());
            })
            .onFailure(testContext::failNow);
        client
            .get(8080, "localhost", "/pets")
            .send()
            .onSuccess(res -> {
                assertEquals(200, res.statusCode());
                final JsonArray pets = res.bodyAsJsonArray();
                assertEquals(1, pets.size());
                assertEquals(1, pets.getJsonObject(0).getInteger("id"));
                assertEquals("Tom", pets.getJsonObject(0).getString("name"));
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }

    @Test
    void getPetById(final Vertx vertx, final VertxTestContext testContext) {
        final WebClient client = WebClient.create(vertx);
        client
            .post(8080, "localhost", "/pets")
            .sendJsonObject(
                new JsonObject()
                    .put("id", 1)
                    .put("name", "Tom"))
            .onSuccess(res -> {
                assertEquals(200, res.statusCode());
            })
            .onFailure(testContext::failNow);
        client
            .get(8080, "localhost", "/pets/1")
            .send()
            .onSuccess(res -> {
                assertEquals(200, res.statusCode());
                final JsonObject pet = res.bodyAsJsonObject();
                assertEquals(1, pet.getInteger("id"));
                assertEquals("Tom", pet.getString("name"));
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
}

