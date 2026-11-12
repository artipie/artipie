/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.test.vertxmain;

import com.artipie.VertxMain;

public class TestVertxMain implements AutoCloseable {

    private final int port;
    private final int apiPort;
    private final VertxMain server;

    public TestVertxMain(int port, VertxMain server) {
        this(port, -1, server);
    }

    public TestVertxMain(int port, int apiPort, VertxMain server) {
        this.port = port;
        this.apiPort = apiPort;
        this.server = server;
    }

    public int port() {
        return port;
    }

    public int apiPort() {
        return apiPort;
    }

    @Override
    public void close() {
        server.stop();
    }
}
