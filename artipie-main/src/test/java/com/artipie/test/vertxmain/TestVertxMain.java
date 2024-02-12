/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.test.vertxmain;

import com.artipie.VertxMain;

public class TestVertxMain {

    private final int port;
    private final VertxMain server;

    public TestVertxMain(int port, VertxMain server) {
        this.port = port;
        this.server = server;
    }

    public int port() {
        return port;
    }

    public void stop() {
        server.stop();
    }
}
