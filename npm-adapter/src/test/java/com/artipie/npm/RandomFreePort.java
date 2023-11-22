/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.npm;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Provides random free port to use in tests.
 * @since 0.6
 */
@SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
public final class RandomFreePort {
    /**
     * Random free port.
     */
    private final int port;

    /**
     * Ctor.
     * @throws IOException if fails to open port
     */
    public RandomFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            this.port = socket.getLocalPort();
        }
    }

    /**
     * Returns free port.
     * @return Free port
     */
    public int value() {
        return this.port;
    }
}
