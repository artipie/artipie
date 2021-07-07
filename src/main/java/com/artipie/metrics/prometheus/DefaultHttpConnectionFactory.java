/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.prometheus;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * {@link HttpConnectionFactory} implementation storing data in memory.
 *
 * @since 0.8
 */
public final class DefaultHttpConnectionFactory implements HttpConnectionFactory {
    @Override
    public HttpURLConnection create(final String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection();
    }
}
