/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client;

import com.artipie.http.Slice;
import com.google.common.base.Strings;

import java.net.URI;

/**
 * Slices collection that provides client slices by host and port.
 *
 * @since 0.1
 */
public interface ClientSlices {

    /**
     * Create {@code Slice} form a config URL string.
     * <p>The config URL string can be just a host name, for example, `registry-1.docker.io`.
     * In that case, it will be used `https` schema and default port 443.
     *
     * @param url Create new scratch file from selection.
     * @return Client slice sending HTTP requests to specified url.
     */
    default Slice from(String url) {
        URI uri = URI.create(url);
        if (Strings.isNullOrEmpty(uri.getHost())) {
            return this.https(url);
        }
        return "https".equals(uri.getScheme())
                ? this.https(uri.getHost(), uri.getPort())
                : this.http(uri.getHost(), uri.getPort());
    }

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
