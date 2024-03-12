/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

/**
 * Provides random free port.
 */
public final class RandomFreePort {
    /**
     * Returns free port.
     *
     * @return Free port.
     */
    public static int get() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }
}
