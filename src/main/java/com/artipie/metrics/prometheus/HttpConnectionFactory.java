/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.prometheus;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Implementation storing data in memory.
 *
 * @since 0.8
 */
public interface HttpConnectionFactory {
    /**
     * Current counter value.
     *
     * @param url Is OK
     * @return Connection is OK
     * @throws IOException Is OK
     */
    HttpURLConnection create(String url) throws IOException;
}
