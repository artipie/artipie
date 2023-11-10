/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.npm.misc;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

/**
 * NextSafeAvailablePort.
 *
 * @since 0.1
 */
public class NextSafeAvailablePort {

    /**
     * The minimum number of server port number as first non-privileged port.
     */
    private static final int MIN_PORT = 1024;

    /**
     * The maximum number of server port number.
     */
    private static final int MAX_PORT = 49_151;

    /**
     * The first and minimum port to scan for availability.
     */
    private final int from;

    /**
     * Ctor.
     */
    public NextSafeAvailablePort() {
        this(NextSafeAvailablePort.MIN_PORT);
    }

    /**
     * Ctor.
     *
     * @param from Port to start scan from
     */
    public NextSafeAvailablePort(final int from) {
        this.from = from;
    }

    /**
     * Gets the next available port starting at a port.
     *
     * @return Next available port
     * @throws IllegalArgumentException if there are no ports available
     */
    public int value() {
        if (this.from < NextSafeAvailablePort.MIN_PORT
            || this.from > NextSafeAvailablePort.MAX_PORT) {
            throw new IllegalArgumentException(
                String.format(
                    "Invalid start port: %d", this.from
                )
            );
        }
        for (int port = this.from; port <= NextSafeAvailablePort.MAX_PORT; port += 1) {
            if (available(port)) {
                return port;
            }
        }
        throw new IllegalArgumentException(
            String.format(
                "Could not find an available port above %d", this.from
            )
        );
    }

    /**
     * Checks to see if a specific port is available.
     *
     * @param port The port to check for availability
     * @return If the ports is available
     * @checkstyle ReturnCountCheck (50 lines)
     * @checkstyle FinalParametersCheck (50 lines)
     * @checkstyle EmptyCatchBlock (50 lines)
     * @checkstyle MethodBodyCommentsCheck (50 lines)
     * @checkstyle RegexpSinglelineCheck (50 lines)
     */
    @SuppressWarnings({"PMD.EmptyCatchBlock", "PMD.OnlyOneReturn"})
    private static boolean available(final int port) {
        ServerSocket sersock = null;
        DatagramSocket dgrmsock = null;
        try {
            sersock = new ServerSocket(port);
            sersock.setReuseAddress(true);
            dgrmsock = new DatagramSocket(port);
            dgrmsock.setReuseAddress(true);
            return true;
        } catch (IOException exp) {
            // should not be thrown
        } finally {
            if (dgrmsock != null) {
                dgrmsock.close();
            }
            if (sersock != null) {
                try {
                    sersock.close();
                } catch (IOException exp) {
                    // should not be thrown
                }
            }
        }
        return false;
    }
}
