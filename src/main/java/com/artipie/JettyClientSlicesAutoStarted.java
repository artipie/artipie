/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.jetty.JettyClientSlices;

/**
 * Jetty client slice started right now.
 * @since 1.0
 */
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.StaticAccessToStaticFields"})
public final class JettyClientSlicesAutoStarted implements ClientSlices {

    /**
     * HTTP client wrapped.
     */
    private final JettyClientSlices origin;

    /**
     * Ctor.
     */
    public JettyClientSlicesAutoStarted() {
        this.origin = JettyClientSlicesAutoStarted.build();
    }

    @Override
    public Slice http(final String host) {
        return this.origin.http(host);
    }

    @Override
    public Slice http(final String host, final int port) {
        return this.http(host, port);
    }

    @Override
    public Slice https(final String host) {
        return this.origin.https(host);
    }

    @Override
    public Slice https(final String host, final int port) {
        return this.origin.https(host, port);
    }

    /**
     * Builds an HTTP client instance.
     * @return HTTP client.
     */
    private static JettyClientSlices build() {
        final JettyClientSlices http = new JettyClientSlices(new HttpClientSettings());
        try {
            http.start();
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception err) {
            throw new IllegalStateException(err);
        }
        return http;
    }
}
