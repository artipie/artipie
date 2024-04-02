/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.junit;

import com.artipie.docker.Docker;
import com.artipie.docker.http.DockerSlice;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;

/**
 * Docker HTTP server, using provided {@link Docker} instance as back-end.
 */
public final class DockerRepository {

    /**
     * Docker slice.
     */
    private final DockerSlice slice;

    /**
     * Vert.x instance used for running HTTP server.
     */
    private Vertx vertx;

    /**
     * HTTP server instance.
     */
    private VertxSliceServer server;

    /**
     * HTTP server port.
     */
    private int port;

    /**
     * @param docker Docker back-end.
     */
    public DockerRepository(final Docker docker) {
        this(new DockerSlice(docker));
    }

    /**
     * @param slice Docker slice.
     */
    public DockerRepository(final DockerSlice slice) {
        this.slice = slice;
    }

    /**
     * Start the server.
     */
    public void start() {
        this.vertx = Vertx.vertx();
        this.server = new VertxSliceServer(
            this.vertx,
            new LoggingSlice(this.slice)
        );
        Logger.debug(this, "Vertx server is created");
        this.port = this.server.start();
        Logger.debug(this, "Vertx server is listening on port %d now", this.port);
    }

    /**
     * Stop the server releasing all resources.
     */
    public void stop() {
        this.port = 0;
        if (this.server != null) {
            this.server.stop();
        }
        Logger.debug(this, "Vertx server is stopped");
        if (this.vertx != null) {
            this.vertx.close();
        }
        Logger.debug(this, "Vertx instance is destroyed");
    }

    /**
     * Server URL.
     *
     * @return Server URL string.
     */
    public String url() {
        if (this.port == 0) {
            throw new IllegalStateException("Server not started");
        }
        return String.format("localhost:%s", this.port);
    }
}
