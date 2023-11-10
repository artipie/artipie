/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.files;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.client.auth.BasicAuthenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.net.URI;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link FileProxySlice} to verify it works with target requiring authentication.
 *
 * @since 0.6
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class FileProxySliceAuthIT {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Jetty client.
     */
    private final JettyClientSlices client = new JettyClientSlices();

    /**
     * Maven proxy.
     */
    private Slice proxy;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    @BeforeEach
    void setUp() throws Exception {
        final Storage storage = new InMemoryStorage();
        storage.save(new Key.From("foo", "bar"), new Content.From("baz".getBytes()))
            .toCompletableFuture().join();
        final String username = "alice";
        final String password = "qwerty";
        this.server = new VertxSliceServer(
            FileProxySliceAuthIT.VERTX,
            new LoggingSlice(
                new FilesSlice(
                    storage,
                    new PolicyByUsername(username),
                    new Authentication.Single(username, password),
                    FilesSlice.ANY_REPO, Optional.empty()
                )
            )
        );
        final int port = this.server.start();
        this.client.start();
        this.proxy = new LoggingSlice(
            new FileProxySlice(
                this.client,
                URI.create(String.format("http://localhost:%d", port)),
                new BasicAuthenticator(username, password),
                new InMemoryStorage()
            )
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        this.client.stop();
        this.server.stop();
    }

    @Test
    void shouldGet() {
        MatcherAssert.assertThat(
            this.proxy.response(
                new RequestLine(RqMethod.GET, "/foo/bar").toString(), Headers.EMPTY, Content.EMPTY
            ),
            new RsHasStatus(RsStatus.OK)
        );
    }
}
