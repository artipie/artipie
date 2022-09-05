/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.nuget.RandomFreePort;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for {@link RepositoryRest}.
 * @since 0.26
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RepositoryRestTest {

    /**
     * Test storage.
     */
    private static BlockingStorage asto;

    /**
     * Service host.
     */
    private static final String HOST = "localhost";

    /**
     * Service port.
     */
    private static int port;

    /**
     * Maximum awaiting time duration of port availability.
     * @checkstyle MagicNumberCheck (10 lines)
     */
    private static final long MAX_WAIT = Duration.ofMinutes(1).toMillis();

    /**
     * Sleep duration.
     */
    private static final long SLEEP_DURATION = Duration.ofMillis(100).toMillis();

    /**
     * Wait test completion.
     * @checkstyle MagicNumberCheck (3 lines)
     */
    private static final long TEST_TIMEOUT = Duration.ofSeconds(3).toSeconds();

    static void prepare(final Vertx vertx, final VertxTestContext ctx, final String layout)
        throws IOException {
        RepositoryRestTest.port = new RandomFreePort().value();
        RepositoryRestTest.asto = new BlockingStorage(new InMemoryStorage());
        vertx.deployVerticle(
            new RestApi(RepositoryRestTest.asto, layout, RepositoryRestTest.port),
            ctx.succeedingThenComplete()
        );
        waitServer(vertx);
    }

    @Test
    void listsAllRepos(final Vertx vertx, final VertxTestContext ctx)
        throws IOException, ExecutionException, InterruptedException, TimeoutException {
        prepare(vertx, ctx, "org");
        RepositoryRestTest.asto.save(new Key.From("artipie/docker-repo.yaml"), new byte[0]);
        RepositoryRestTest.asto.save(new Key.From("alice/rpm-local.yml"), new byte[0]);
        RepositoryRestTest.asto.save(new Key.From("alice/conda.yml"), new byte[0]);
        vertx.createHttpClient()
            .request(
                HttpMethod.GET, RepositoryRestTest.port, "localhost", "/api/v1/repository/list"
            )
            .compose(req -> req.send().compose(HttpClientResponse::body))
            .onComplete(
                ctx.succeeding(
                    buffer -> ctx.verify(
                        () -> {
                            MatcherAssert.assertThat(
                                buffer.toJsonArray().stream().collect(Collectors.toList()),
                                Matchers.containsInAnyOrder(
                                    "alice/rpm-local", "artipie/docker-repo", "alice/conda"
                                )
                            );
                            ctx.completeNow();
                        }
                    )
                )
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RepositoryRestTest.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    void listsUserRepos(final Vertx vertx, final VertxTestContext ctx)
        throws IOException, ExecutionException, InterruptedException, TimeoutException {
        prepare(vertx, ctx, "org");
        RepositoryRestTest.asto.save(new Key.From("artipie/docker-repo.yaml"), new byte[0]);
        RepositoryRestTest.asto.save(new Key.From("alice/rpm-local.yml"), new byte[0]);
        RepositoryRestTest.asto.save(new Key.From("alice/conda.yml"), new byte[0]);
        vertx.createHttpClient().request(
            HttpMethod.GET, RepositoryRestTest.port,
            "localhost", "/api/v1/repository/list/alice"
        )
            .compose(req -> req.send().compose(HttpClientResponse::body))
            .onComplete(
                ctx.succeeding(
                    buffer -> ctx.verify(
                        () -> {
                            MatcherAssert.assertThat(
                                buffer.toJsonArray().stream().collect(Collectors.toList()),
                                Matchers.containsInAnyOrder("alice/rpm-local", "alice/conda")
                            );
                            ctx.completeNow();
                        }
                    )
                )
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RepositoryRestTest.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    void createRepo(final Vertx vertx, final VertxTestContext ctx)
        throws IOException, ExecutionException, InterruptedException, TimeoutException {
        prepare(vertx, ctx, "flat");
        final JsonObject json = new JsonObject()
            .put(
                "repo", new JsonObject()
                    .put("type", "fs")
                    .put("storage", new JsonObject())
            );
        WebClient.create(vertx)
            .put(
                RepositoryRestTest.port,
                RepositoryRestTest.HOST,
                "/api/v1/repository/newrepo"
            )
            .sendJsonObject(json)
            .onSuccess(
                res -> {
                    MatcherAssert.assertThat(
                        res.statusCode(),
                        // @checkstyle MagicNumberCheck (1 line)
                        Matchers.is(200)
                    );
                    MatcherAssert.assertThat(
                        RepositoryRestTest.asto.exists(new Key.From("newrepo.yml")),
                        Matchers.is(true)
                    );
                    ctx.completeNow();
                }
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RepositoryRestTest.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    void createDuplicateRepo(final Vertx vertx, final VertxTestContext ctx)
        throws IOException, ExecutionException, InterruptedException, TimeoutException {
        prepare(vertx, ctx, "flat");
        RepositoryRestTest.asto.save(new Key.From("newrepo.yaml"), new byte[0]);
        final JsonObject json = new JsonObject()
            .put(
                "repo", new JsonObject()
                    .put("type", "fs")
                    .put("storage", new JsonObject())
            );
        WebClient.create(vertx)
            .put(
                RepositoryRestTest.port,
                RepositoryRestTest.HOST,
                "/api/v1/repository/newrepo"
            )
            .sendJsonObject(json)
            .onSuccess(
                res -> {
                    MatcherAssert.assertThat(
                        res.statusCode(),
                        // @checkstyle MagicNumberCheck (1 line)
                        Matchers.is(409)
                    );
                    ctx.completeNow();
                }
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RepositoryRestTest.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    void createUserRepo(final Vertx vertx, final VertxTestContext ctx)
        throws IOException, ExecutionException, InterruptedException, TimeoutException {
        prepare(vertx, ctx, "org");
        final JsonObject json = new JsonObject()
            .put(
                "repo", new JsonObject()
                    .put("type", "fs")
                    .put("storage", new JsonObject())
            );
        WebClient.create(vertx)
            .put(
                RepositoryRestTest.port,
                RepositoryRestTest.HOST,
                "/api/v1/repository/alice/newrepo"
            )
            .sendJsonObject(json)
            .onSuccess(
                res -> {
                    MatcherAssert.assertThat(
                        res.statusCode(),
                        // @checkstyle MagicNumberCheck (1 line)
                        Matchers.is(200)
                    );
                    MatcherAssert.assertThat(
                        RepositoryRestTest.asto.exists(new Key.From("alice/newrepo.yml")),
                        Matchers.is(true)
                    );
                    ctx.completeNow();
                }
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RepositoryRestTest.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    void createDuplicateUserRepo(final Vertx vertx, final VertxTestContext ctx)
        throws IOException, ExecutionException, InterruptedException, TimeoutException {
        prepare(vertx, ctx, "org");
        RepositoryRestTest.asto.save(new Key.From("alice/newrepo.yaml"), new byte[0]);
        final JsonObject json = new JsonObject()
            .put(
                "repo", new JsonObject()
                    .put("type", "fs")
                    .put("storage", new JsonObject())
            );
        WebClient.create(vertx)
            .put(
                RepositoryRestTest.port,
                RepositoryRestTest.HOST,
                "/api/v1/repository/alice/newrepo"
            )
            .sendJsonObject(json)
            .onSuccess(
                res -> {
                    MatcherAssert.assertThat(
                        res.statusCode(),
                        // @checkstyle MagicNumberCheck (1 line)
                        Matchers.is(409)
                    );
                    ctx.completeNow();
                }
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RepositoryRestTest.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Waits until server port available.
     *
     * @param vertx Vertx instance
     */
    private static void waitServer(final Vertx vertx) {
        final AtomicReference<Boolean> available = new AtomicReference<>(false);
        final NetClient client = vertx.createNetClient();
        final long max = System.currentTimeMillis() + RepositoryRestTest.MAX_WAIT;
        while (!available.get() && System.currentTimeMillis() < max) {
            client.connect(
                RepositoryRestTest.port, RepositoryRestTest.HOST,
                ar -> {
                    if (ar.succeeded()) {
                        available.set(true);
                    }
                }
            );
            if (!available.get()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(RepositoryRestTest.SLEEP_DURATION);
                } catch (final InterruptedException err) {
                    break;
                }
            }
        }
        if (!available.get()) {
            Assertions.fail(
                String.format(
                    "Server's port %s:%s is not reachable",
                    RepositoryRestTest.HOST, RepositoryRestTest.port
                )
            );
        }
    }
}
