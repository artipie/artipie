/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
