/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.http.slice.SliceSimple;
import com.artipie.pypi.PypiDeployment;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.URI;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test for {@link PyProxySlice}.
 * @since 0.7
 */
@DisabledOnOs(OS.WINDOWS)
final class PyProxySliceCacheITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Jetty client.
     */
    private final JettyClientSlices client = new JettyClientSlices();

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Bad vertx slice server instance, always returns 404 status.
     */
    private VertxSliceServer bad;

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Pypi container.
     */
    @RegisterExtension
    private final PypiDeployment container = new PypiDeployment();

    @BeforeEach
    void setUp() throws Exception {
        this.client.start();
        this.storage = new InMemoryStorage();
        this.bad = new VertxSliceServer(
            PyProxySliceCacheITCase.VERTX,
            new SliceSimple(StandardRs.NOT_FOUND)
        );
        this.server = new VertxSliceServer(
            PyProxySliceCacheITCase.VERTX,
            new LoggingSlice(
                new PyProxySlice(
                    this.client,
                    URI.create(String.format("http://localhost:%d", this.bad.start())),
                    this.storage
                )
            ),
            this.container.port()
        );
        this.server.start();
    }

    @Test
    void installsFromCache() throws IOException, InterruptedException {
        new TestResource("pypi_repo/alarmtime-0.1.5.tar.gz")
            .saveTo(this.storage, new Key.From("alarmtime/alarmtime-0.1.5.tar.gz"));
        this.storage.save(
            new Key.From("alarmtime"), new Content.From(this.indexHtml().getBytes())
        ).join();
        MatcherAssert.assertThat(
            this.container.bash(
                String.format(
                    "pip install --index-url %s --verbose --no-deps --trusted-host host.testcontainers.internal \"alarmtime\"",
                    this.container.localAddress()
                )
            ),
            Matchers.containsString("Successfully installed alarmtime-0.1.5")
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        this.client.stop();
        this.server.stop();
        this.bad.stop();
    }

    private String indexHtml() {
        return String.join(
            "\n", "<!DOCTYPE html>",
            "<html>",
            "  <head>",
            "    <title>Links for AlarmTime</title>",
            "  </head>",
            "  <body>",
            "    <h1>Links for AlarmTime</h1>",
            "    <a href=\"/alarmtime/alarmtime-0.1.5.tar.gz\">alarmtime-0.1.5.tar.gz</a><br/>",
            "</body>",
            "</html>"
        );
    }

}
