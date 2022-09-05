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
import io.vertx.core.net.NetClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
        vertx.deployVerticle(
            new RestApi(this.asto, this.layout(), this.prt),
            context.succeedingThenComplete()
        );
        this.waitServer(vertx);
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
}
