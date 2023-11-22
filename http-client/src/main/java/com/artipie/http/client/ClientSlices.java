/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client;

import com.artipie.http.Slice;

/**
 * Slices collection that provides client slices by host and port.
 *
 * @since 0.1
 */
public interface ClientSlices {

    /**
     * Create client slice sending HTTP requests to specified host on port 80.
     *
     * @param host Host name.
     * @return Client slice.
     */
    Slice http(String host);

    /**
     * Create client slice sending HTTP requests to specified host.
     *
     * @param host Host name.
     * @param port Target port.
     * @return Client slice.
     */
    Slice http(String host, int port);

    /**
     * Create client slice sending HTTPS requests to specified host on port 443.
     *
     * @param host Host name.
     * @return Client slice.
     */
    Slice https(String host);

    /**
     * Create client slice sending HTTPS requests to specified host.
     *
     * @param host Host name.
     * @param port Target port.
     * @return Client slice.
     */
    Slice https(String host, int port);
}
