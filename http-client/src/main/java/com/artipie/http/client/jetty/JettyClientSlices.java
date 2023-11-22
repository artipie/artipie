/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.jetty;

import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.Settings;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.client.transport.HttpClientTransportOverHTTP3;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * ClientSlices implementation using Jetty HTTP client as back-end.
 * <code>start()</code> method should be called before sending responses to initialize
 * underlying client. <code>stop()</code> methods should be used to release resources
 * and stop requests in progress.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class JettyClientSlices implements ClientSlices {

    /**
     * Default HTTP port.
     */
    private static final int HTTP_PORT = 80;

    /**
     * Default HTTPS port.
     */
    private static final int HTTPS_PORT = 443;

    /**
     * HTTP client.
     */
    private final HttpClient clnt;

    /**
     * Ctor.
     */
    public JettyClientSlices() {
        this(new Settings.Default());
    }

    /**
     * Ctor.
     *
     * @param settings Settings.
     */
    public JettyClientSlices(final Settings settings) {
        this.clnt = create(settings);
    }

    /**
     * Prepare for usage.
     *
     * @throws Exception In case of any errors starting.
     */
    public void start() throws Exception {
        this.clnt.start();
    }

    /**
     * Release used resources and stop requests in progress.
     *
     * @throws Exception In case of any errors stopping.
     */
    public void stop() throws Exception {
        this.clnt.stop();
    }

    @Override
    public Slice http(final String host) {
        return this.slice(false, host, JettyClientSlices.HTTP_PORT);
    }

    @Override
    public Slice http(final String host, final int port) {
        return this.slice(false, host, port);
    }

    @Override
    public Slice https(final String host) {
        return this.slice(true, host, JettyClientSlices.HTTPS_PORT);
    }

    @Override
    public Slice https(final String host, final int port) {
        return this.slice(true, host, port);
    }

    /**
     * Create slice backed by client.
     *
     * @param secure Secure connection flag.
     * @param host Host name.
     * @param port Port.
     * @return Client slice.
     */
    private Slice slice(final boolean secure, final String host, final int port) {
        return new JettyClientSlice(this.clnt, secure, host, port);
    }

    /**
     * Creates {@link HttpClient} from {@link Settings}.
     *
     * @param settings Settings.
     * @return HTTP client built from settings.
     */
    private static HttpClient create(final Settings settings) {
        final HttpClient result;
        if (settings.http3()) {
            result = new HttpClient(new HttpClientTransportOverHTTP3(new HTTP3Client()));
        } else {
            result = new HttpClient();
        }
        final SslContextFactory.Client factory = new SslContextFactory.Client();
        factory.setTrustAll(settings.trustAll());
        result.setSslContextFactory(factory);
        settings.proxy().ifPresent(
            proxy -> result.getProxyConfiguration().addProxy(
                new HttpProxy(new Origin.Address(proxy.host(), proxy.port()), proxy.secure())
            )
        );
        result.setFollowRedirects(settings.followRedirects());
        if (settings.connectTimeout() <= 0) {
            /* @checkstyle MethodBodyCommentsCheck (1 line)
             * Jetty client does not treat zero value as infinite timeout in non-blocking mode.
             * Instead it timeouts the connection instantly.
             * That has been tested in version org.eclipse.jetty:jetty-client:9.4.30.v20200611.
             * See "org.eclipse.jetty.io.ManagedSelector.Connect" class constructor
             * and "run()" method for details.
             * Issue was reported, see https://github.com/eclipse/jetty.project/issues/5150
             */
            result.setConnectBlocking(true);
        }
        result.setConnectTimeout(settings.connectTimeout());
        result.setIdleTimeout(settings.idleTimeout());
        return result;
    }
}
