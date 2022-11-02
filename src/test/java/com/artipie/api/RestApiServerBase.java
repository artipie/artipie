/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.api.ssl.KeyStore;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.Authentication;
import com.artipie.nuget.RandomFreePort;
import com.artipie.settings.Layout;
import com.artipie.settings.Settings;
import com.artipie.settings.cache.SettingsCaches;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base class for Rest API tests. When creating test for rest API verticle, extend this class
 * and implement {@link RestApiServerBase#layout()} method.
 * @since 0.27
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
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
     * Artipie authentication, this method can be overridden if necessary.
     * @return Authentication instance.
     * @checkstyle NonStaticMethodCheck (5 lines)
     */
    Authentication auth() {
        return Authentication.ANONYMOUS;
    }

    /**
     * Create the SSL KeyStore.
     * Creates instance of KeyStore based on Artipie yaml-configuration.
     * @return KeyStore.
     * @throws IOException During yaml creation
     * @checkstyle NonStaticMethodCheck (5 lines)
     */
    Optional<KeyStore> keyStore() throws IOException {
        return Optional.empty();
    }

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
        final InMemoryStorage storage = new InMemoryStorage();
        this.asto = new BlockingStorage(storage);
        this.caches = new SettingsCaches.Fake();
        vertx.deployVerticle(
            new RestApi(
                this.caches, storage, this.layout(), this.prt, Optional.of(ManageUsersTest.KEY),
                this.auth(),
                this.keyStore(),
                new Settings.Fake(Layout.valueOf(this.layout()))
            ),
            context.succeedingThenComplete()
        );
        this.waitServer(vertx);
    }

    /**
     * Perform the request and check the result. In this request auth token for username
     * `anonymous` is used, issued to be valid forever.
     * @param vertx Text vertx server instance
     * @param ctx Vertx Test Context
     * @param rqs Request parameters: method and path
     * @param assertion Test assertion
     * @throws Exception On error
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    final void requestAndAssert(final Vertx vertx, final VertxTestContext ctx,
        final TestRequest rqs, final Consumer<HttpResponse<Buffer>> assertion) throws Exception {
        this.requestAndAssert(
            vertx, ctx, rqs,
            //@checkstyle LineLengthCheck (1 line)
            Optional.of("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbm9ueW1vdXMifQ.maJSTCP0koQO-lCx1cs4sBLepSxFMJ8liAqUQH_9-bY"),
            assertion
        );
    }

    /**
     * Perform the request and check the result.
     * @param vertx Text vertx server instance
     * @param ctx Vertx Test Context
     * @param rqs Request parameters: method and path
     * @param token Jwt auth token
     * @param assertion Test assertion
     * @throws Exception On error
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    final void requestAndAssert(final Vertx vertx, final VertxTestContext ctx,
        final TestRequest rqs, final Optional<String> token,
        final Consumer<HttpResponse<Buffer>> assertion) throws Exception {
        final HttpRequest<Buffer> request = WebClient.create(vertx, this.webClientOptions())
            .request(rqs.method, this.port(), RestApiServerBase.HOST, rqs.path);
        token.ifPresent(request::bearerTokenAuthentication);
        rqs.body
            .map(request::sendJsonObject)
            .orElseGet(request::send)
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
     * Creates web client options.
     * @return WebClientOptions instance.
     * @throws IOException During yaml creation
     */
    final WebClientOptions webClientOptions() throws IOException {
        final WebClientOptions options = new WebClientOptions();
        if (this.keyStore().isPresent() && this.keyStore().get().enabled()) {
            options.setSsl(true).setTrustAll(true);
        }
        return options;
    }

    /**
     * Obtain jwt auth token for given username and password.
     * @param vertx Text vertx server instance
     * @param ctx Vertx Test Context
     * @param name Username
     * @param pass Password
     * @return Jwt token
     * @throws Exception On error
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    final AtomicReference<String> getToken(
        final Vertx vertx, final VertxTestContext ctx, final String name, final String pass
    ) throws Exception {
        final AtomicReference<String> token = new AtomicReference<>();
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.POST, "/api/v1/oauth/token",
                new JsonObject().put("name", name).put("pass", pass)
            ), Optional.empty(),
            response -> {
                MatcherAssert.assertThat(
                    "Failed to get token",
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                token.set(((JsonObject) response.body().toJson()).getString("token"));
            }
        );
        return token;
    }

    /**
     * Waits until server port available.
     *
     * @param vertx Vertx instance
     */
    final void waitServer(final Vertx vertx) {
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
     * Awaiting of action during maximum 5 seconds.
     * Allows to wait result of action during period of time.
     * @param action Action
     * @return Result of action
     * @checkstyle MagicNumberCheck (15 lines)
     * @checkstyle NonStaticMethodCheck (15 lines)
     */
    final Boolean waitCondition(final Supplier<Boolean> action) {
        final long max = System.currentTimeMillis() + RestApiServerBase.MAX_WAIT;
        boolean res;
        do {
            res = action.get();
            if (res) {
                break;
            } else {
                try {
                    TimeUnit.MILLISECONDS.sleep(RestApiServerBase.SLEEP_DURATION);
                } catch (final InterruptedException exc) {
                    break;
                }
            }
        } while (System.currentTimeMillis() < max);
        if (!res) {
            res = action.get();
        }
        return res;
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

        @Override
        public String toString() {
            return String.format(
                "TestRequest: method='%s', path='%s', body='%s'", this.method, this.path, this.body
            );
        }
    }
}
