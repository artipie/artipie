/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.cache.FromStorageCache;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.scheduling.ProxyArtifactEvent;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MavenProxySlice} to verify it can work with central.
 */
final class MavenProxySliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Jetty client.
     */
    private final JettyClientSlices client = new JettyClientSlices();

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Server port.
     */
    private int port;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Artifact events queue.
     */
    private Queue<ProxyArtifactEvent> events;

    @BeforeEach
    void setUp() throws Exception {
        this.client.start();
        this.storage = new InMemoryStorage();
        this.events = new LinkedList<>();
        this.server = new VertxSliceServer(
            MavenProxySliceITCase.VERTX,
            new LoggingSlice(
                new MavenProxySlice(
                    this.client,
                    URI.create("https://repo.maven.apache.org/maven2"),
                    Authenticator.ANONYMOUS,
                    new FromStorageCache(this.storage),
                    Optional.of(this.events),
                    "my-maven-proxy"
                )
            )
        );
        this.port = this.server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        this.client.stop();
        this.server.stop();
    }

    @Test
    void downloadsJarFromCentralAndCachesIt() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%s/args4j/args4j/2.32/args4j-2.32.jar", this.port)
        ).toURL().openConnection();
        con.setRequestMethod("GET");
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(RsStatus.OK.code())
        );
        MatcherAssert.assertThat(
            "Jar was saved to storage",
            this.storage.exists(new Key.From("args4j/args4j/2.32/args4j-2.32.jar")).join(),
            new IsEqual<>(true)
        );
        con.disconnect();
        MatcherAssert.assertThat("Event was added to queue", this.events.size() == 1);
    }

    @Test
    void downloadsJarFromCache() throws Exception {
        new TestResource("com/artipie/helloworld")
            .addFilesTo(this.storage, new Key.From("com", "artipie", "helloworld"));
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format(
                "http://localhost:%s/com/artipie/helloworld/0.1/helloworld-0.1.jar", this.port
            )
        ).toURL().openConnection();
        con.setRequestMethod("GET");
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(RsStatus.OK.code())
        );
        con.disconnect();
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    @Test
    void downloadJarFromCentralAndCacheFailsWithNotFound() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%s/notfoundexample", this.port)
        ).toURL().openConnection();
        con.setRequestMethod("GET");
        MatcherAssert.assertThat(
            con.getResponseCode(),
            new IsEqual<>(RsStatus.NOT_FOUND.code())
        );
        con.disconnect();
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    @Test
    void headRequestWorks() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%s/args4j/args4j/2.32/args4j-2.32.pom", this.port)
        ).toURL().openConnection();
        con.setRequestMethod("HEAD");
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(RsStatus.OK.code())
        );
        MatcherAssert.assertThat(
            "Headers are returned",
            con.getHeaderFields(),
            Matchers.allOf(
                Matchers.hasKey("Content-Type"),
                Matchers.hasKey("Last-Modified"),
                Matchers.hasKey("ETag"),
                Matchers.hasKey("X-Checksum-MD5"),
                Matchers.hasKey("X-Checksum-SHA1")
            )
        );
        con.disconnect();
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    @Test
    void checksumRequestWorks() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%s/args4j/args4j/2.32/args4j-2.32.pom.md5", this.port)
        ).toURL().openConnection();
        con.setRequestMethod("GET");
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(RsStatus.OK.code())
        );
        con.disconnect();
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

}
