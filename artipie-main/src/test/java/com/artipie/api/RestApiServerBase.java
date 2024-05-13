/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.api.ssl.KeyStore;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.nuget.RandomFreePort;
import com.artipie.security.policy.Policy;
import com.artipie.settings.ArtipieSecurity;
import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.test.TestArtipieCaches;
import com.artipie.test.TestStoragesCache;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
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
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for Rest API tests. When creating test for rest API verticle, extend this class.
 * @since 0.27
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
@Execution(ExecutionMode.CONCURRENT)
public class RestApiServerBase {

    /**
     * Max wait time for condition in seconds.
     */
    public static final int MAX_WAIT_TIME = 5;

    /**
     * Wait test completion.
     */
    static final long TEST_TIMEOUT = Duration.ofSeconds(3).toSeconds();

    /**
     * Service host.
     */
    static final String HOST = "localhost";

    /**
     * Maximum awaiting time duration of port availability.
     */
    private static final long MAX_WAIT = Duration.ofMinutes(1).toMillis();

    /**
     * Sleep duration.
     */
    private static final long SLEEP_DURATION = Duration.ofMillis(100).toMillis();

    /**
     * Test security storage.
     */
    protected Storage ssto;

    /**
     * Server port.
     */
    private int prt;

    /**
     * Test storage.
     */
    private BlockingStorage asto;

    /**
     * Test artipie`s caches.
     */
    private ArtipieCaches caches;

    /**
     * Artipie authentication, this method can be overridden if necessary.
     * @return Authentication instance.
     */
    ArtipieSecurity auth() {
        return new ArtipieSecurity() {
            @Override
            public Authentication authentication() {
                return (name, pswd) -> Optional.of(new AuthUser("artipie", "test"));
            }

            @Override
            public Policy<?> policy() {
                return Policy.FREE;
            }

            @Override
            public Optional<Storage> policyStorage() {
                return Optional.of(RestApiServerBase.this.ssto);
            }
        };
    }

    /**
     * Create the SSL KeyStore.
     * Creates instance of KeyStore based on Artipie yaml-configuration.
     * @return KeyStore.
     * @throws IOException During yaml creation
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
     * Save bytes into test storage with provided key.
     * @param key The key
     * @param data Data to save
     */
    final void saveIntoSecurityStorage(final Key key, final byte[] data) {
        new BlockingStorage(this.ssto).save(key, data);
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
     * Get test security storage.
     * @return Instance of {@link BlockingStorage}
     */
    final BlockingStorage securityStorage() {
        return new BlockingStorage(this.ssto);
    }

    /**
     * Get settings caches.
     * @return Instance of {@link ArtipieCaches}
     */
    final ArtipieCaches settingsCaches() {
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
        this.caches = new TestArtipieCaches();
        this.ssto = new InMemoryStorage();
        vertx.deployVerticle(
            new RestApi(
                this.caches, storage, this.prt,
                this.auth(),
                this.keyStore(),
                JWTAuth.create(
                    vertx, new JWTAuthOptions().addPubSecKey(
                        new PubSecKeyOptions().setAlgorithm("HS256").setBuffer("some secret")
                    )
                ),
                Optional.empty()
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
     */
    final void requestAndAssert(final Vertx vertx, final VertxTestContext ctx,
        final TestRequest rqs, final Consumer<HttpResponse<Buffer>> assertion) throws Exception {
        this.requestAndAssert(
            vertx, ctx, rqs,
            Optional.of("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcnRpcGllIiwiY29udGV4dCI6InRlc3QiLCJpYXQiOjE2ODIwODgxNTh9.QjQPLQ0tQFbiRIWpE-GUtUFXvUXvXP4p7va_DOBHjTM"),
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
     * Asserts that storages cache was invalidated.
     */
    void assertStorageCacheInvalidated() {
        MatcherAssert.assertThat(
            "Storages cache was invalidated",
            ((TestStoragesCache) this.settingsCaches().storagesCache())
                .wasInvalidated()
        );
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
