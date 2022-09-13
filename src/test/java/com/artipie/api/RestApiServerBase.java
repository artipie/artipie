/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.nuget.RandomFreePort;
import com.artipie.settings.cache.SettingsCaches;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base class for Rest API tests. When creating test for rest API verticle, extend this class
 * and implement {@link RestApiServerBase#layout()} method.
 * @since 0.27
 */
@ExtendWith(VertxExtension.class)
public abstract class RestApiServerBase {

    /**
     * Wait test completion.
     * @checkstyle MagicNumberCheck (3 lines)
     */
    static final long TEST_TIMEOUT = Duration.ofSeconds(3).toSeconds();

    /**
     * Service host.
     */
    static final String HOST = "localhost";

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
     * Server port.
     */
    private int prt;

    /**
     * Test storage.
     */
    private BlockingStorage asto;

    /**
     * Test settings caches.
     */
    private SettingsCaches caches;

    /**
     * Artipie layout.
     * @return String layout: org or flat.
     */
    abstract String layout();

    /**
     * Save bytes into test storage with provided key.
     * @param key The key
     * @param data Data to save
     */
    final void save(final Key key, final byte[] data) {
        this.asto.save(key, data);
    }

    /**
     * Get test server port.
     * @return The port int value
     */
    final int port() {
        return this.prt;
    }

    /**
     * Get test storage.
     * @return Instance of {@link BlockingStorage}
     */
    final BlockingStorage storage() {
        return this.asto;
    }

    /**
     * Get settings caches.
     * @return Instance of {@link SettingsCaches}
     */
    final SettingsCaches settingsCaches() {
        return this.caches;
    }

    /**
     * Before each method searches for free port, creates test storage instance, starts and waits
     * for test verts server to be up and running.
     * @param vertx Vertx instance
     * @param context Test context
     * @throws Exception On any error
     */
    @BeforeEach
    final void beforeEach(final Vertx vertx, final VertxTestContext context) throws Exception {
        this.prt = new RandomFreePort().value();
        this.asto = new BlockingStorage(new InMemoryStorage());
        this.caches = new SettingsCaches.Fake();
        vertx.deployVerticle(
            new RestApi(
                this.caches, this.asto, this.layout(), this.prt, Optional.of(ManageUsersTest.KEY)
            ),
            context.succeedingThenComplete()
        );
        this.waitServer(vertx);
    }

    /**
     * Perform the request and check the result.
     * @param vertx Text vertx server instance
     * @param ctx Vertx Test Context
     * @param rqs Request parameters: method and path
     * @param assertion Test assertion
     * @throws Exception On error
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    final void requestAndAssert(final Vertx vertx, final VertxTestContext ctx,
        final TestRequest rqs, final Consumer<HttpResponse<Buffer>> assertion) throws Exception {
        final HttpRequest<Buffer> request = WebClient.create(vertx)
            .request(rqs.method, this.port(), RestApiServerBase.HOST, rqs.path);
        rqs.body.map(request::sendJsonObject)
            .orElse(request.send())
            .onSuccess(
                res -> {
                    assertion.accept(res);
                    ctx.completeNow();
                }
            )
            .onFailure(ctx::failNow)
            .toCompletionStage().toCompletableFuture()
            .get(RestApiServerBase.TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Waits until server port available.
     *
     * @param vertx Vertx instance
     */
    private void waitServer(final Vertx vertx) {
        final AtomicReference<Boolean> available = new AtomicReference<>(false);
        final NetClient client = vertx.createNetClient();
        final long max = System.currentTimeMillis() + RestApiServerBase.MAX_WAIT;
        while (!available.get() && System.currentTimeMillis() < max) {
            client.connect(
                this.prt, RestApiServerBase.HOST,
                ar -> {
                    if (ar.succeeded()) {
                        available.set(true);
                    }
                }
            );
            if (!available.get()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(RestApiServerBase.SLEEP_DURATION);
                } catch (final InterruptedException err) {
                    break;
                }
            }
        }
        if (!available.get()) {
            Assertions.fail(
                String.format(
                    "Server's port %s:%s is not reachable",
                    RestApiServerBase.HOST, this.prt
                )
            );
        }
    }

    /**
     * Test request.
     * @since 0.27
     */
    static final class TestRequest {

        /**
         * Http method.
         */
        private final HttpMethod method;

        /**
         * Request path.
         */
        private final String path;

        /**
         * Request json body.
         */
        private Optional<JsonObject> body;

        /**
         * Ctor.
         * @param method Http method
         * @param path Request path
         * @param body Request body
         */
        TestRequest(final HttpMethod method, final String path, final Optional<JsonObject> body) {
            this.method = method;
            this.path = path;
            this.body = body;
        }

        /**
         * Ctor.
         * @param method Http method
         * @param path Request path
         * @param body Request body
         */
        TestRequest(final HttpMethod method, final String path, final JsonObject body) {
            this(method, path, Optional.of(body));
        }

        /**
         * Ctor.
         * @param method Http method
         * @param path Request path
         */
        TestRequest(final HttpMethod method, final String path) {
            this(method, path, Optional.empty());
        }

        /**
         * Ctor with default GET method.
         * @param path Request path
         */
        TestRequest(final String path) {
            this(HttpMethod.GET, path);
        }
    }
}
