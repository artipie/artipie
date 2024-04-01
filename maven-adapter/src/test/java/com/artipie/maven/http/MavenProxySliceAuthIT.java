/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.Authentication;
import com.artipie.http.client.auth.BasicAuthenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.net.URI;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MavenProxySlice} to verify it works with target requiring authentication.
 *
 * @since 0.7
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class MavenProxySliceAuthIT {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Username and password.
     */
    private static final Pair<String, String> USER = new ImmutablePair<>("alice", "qwerty");

    /**
     * Jetty client.
     */
    private final JettyClientSlices client = new JettyClientSlices();

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Origin server port.
     */
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        final Storage storage = new InMemoryStorage();
        new TestResource("com/artipie/helloworld").addFilesTo(
            storage,
            new Key.From("com", "artipie", "helloworld")
        );
        this.server = new VertxSliceServer(
            MavenProxySliceAuthIT.VERTX,
            new LoggingSlice(
                new MavenSlice(
                    storage,
                    new PolicyByUsername(MavenProxySliceAuthIT.USER.getKey()),
                    new Authentication.Single(
                        MavenProxySliceAuthIT.USER.getKey(), MavenProxySliceAuthIT.USER.getValue()
                    ),
                    "test",
                    Optional.empty()
                )
            )
        );
        this.port = this.server.start();
        this.client.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        this.client.stop();
        this.server.stop();
    }

    @Test
    void shouldGet() {
        MatcherAssert.assertThat(
            new MavenProxySlice(
                this.client,
                URI.create(String.format("http://localhost:%d", this.port)),
                new BasicAuthenticator(
                    MavenProxySliceAuthIT.USER.getKey(), MavenProxySliceAuthIT.USER.getValue()
                )
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, "/com/artipie/helloworld/0.1/helloworld-0.1.pom")
            )
        );
    }

    @Test
    void shouldNotGetWithWrongUser() {
        MatcherAssert.assertThat(
            new MavenProxySlice(
                this.client,
                URI.create(String.format("http://localhost:%d", this.port)),
                new BasicAuthenticator("any", "any")
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/com/artipie/helloworld/0.1/helloworld-0.1.pom")
            )
        );
    }
}
