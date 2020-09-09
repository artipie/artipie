/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client settings from system environment.
 *
 * @since 0.9
 */
final class HttpClientSettings implements com.artipie.http.client.Settings {

    /**
     * Proxy host system property key.
     */
    static final String PROXY_HOST = "http.proxyHost";

    /**
     * Proxy port system property key.
     */
    static final String PROXY_PORT = "http.proxyPort";

    @Override
    public Optional<Proxy> proxy() {
        final Optional<Proxy> result;
        final String host = System.getProperty(HttpClientSettings.PROXY_HOST);
        final String port = System.getProperty(HttpClientSettings.PROXY_PORT);
        if (Strings.isNullOrEmpty(host) || Strings.isNullOrEmpty(port)) {
            result = Optional.empty();
        } else {
            result = Optional.of(new Proxy.Simple(false, host, Integer.parseInt(port)));
        }
        return result;
    }

    @Override
    public boolean trustAll() {
        return "true".equals(System.getenv("SSL_TRUSTALL"));
    }

    @Override
    public boolean followRedirects() {
        return true;
    }

    @Override
    public long connectTimeout() {
        final int seconds = 15;
        return TimeUnit.SECONDS.toMillis(seconds);
    }

    @Override
    public long idleTimeout() {
        final int seconds = 30;
        return TimeUnit.SECONDS.toMillis(seconds);
    }
}
