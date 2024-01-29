/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.jetty;

import com.artipie.ArtipieException;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.HttpClientSettings;
import com.google.common.base.Strings;
import org.eclipse.jetty.client.BasicAuthentication;
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
        this(new HttpClientSettings());
    }

    /**
     * Ctor.
     *
     * @param settings Settings.
     */
    public JettyClientSlices(final HttpClientSettings settings) {
        this.clnt = create(settings);
    }

    /**
     * Prepare for usage.
     */
    public void start() {
        try {
            this.clnt.start();
        } catch (Exception e) {
            throw new ArtipieException(e);
        }
    }

    /**
     * Release used resources and stop requests in progress.
     */
    public void stop() {
        try {
            this.clnt.stop();
        } catch (Exception e) {
            throw new ArtipieException(e);
        }
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
     * Creates {@link HttpClient} from {@link HttpClientSettings}.
     *
     * @param settings Settings.
     * @return HTTP client built from settings.
     */
    private static HttpClient create(final HttpClientSettings settings) {
        final HttpClient result;
        if (settings.http3()) {
            result = new HttpClient(new HttpClientTransportOverHTTP3(new HTTP3Client()));
        } else {
            result = new HttpClient();
        }
        final SslContextFactory.Client factory = new SslContextFactory.Client();
        factory.setTrustAll(settings.trustAll());
        if (!Strings.isNullOrEmpty(settings.jksPath())) {
            factory.setKeyStoreType("jks");
            factory.setKeyStorePath(settings.jksPath());
            factory.setKeyStorePassword(settings.jksPwd());
        }
        result.setSslContextFactory(factory);
        settings.proxies().forEach(
            proxy -> {
                if (!Strings.isNullOrEmpty(proxy.basicRealm())) {
                    result.getAuthenticationStore().addAuthentication(
                        new BasicAuthentication(
                            proxy.uri(), proxy.basicRealm(), proxy.basicUser(), proxy.basicPwd()
                        )
                    );
                }
                result.getProxyConfiguration().addProxy(
                    new HttpProxy(new Origin.Address(proxy.host(), proxy.port()), proxy.secure())
                );
            }
        );
        result.setFollowRedirects(settings.followRedirects());
        result.setConnectTimeout(settings.connectTimeout());
        result.setIdleTimeout(settings.idleTimeout());
        return result;
    }
}
