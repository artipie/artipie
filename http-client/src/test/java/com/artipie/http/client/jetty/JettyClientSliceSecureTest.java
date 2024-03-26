/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.jetty;

import com.artipie.asto.test.TestResource;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Tests for {@link JettyClientSlice} with HTTPS server.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class JettyClientSliceSecureTest extends JettyClientSliceTest {

    @Override
    HttpClient newHttpClient() {
        final SslContextFactory.Client factory = new SslContextFactory.Client();
        factory.setTrustAll(true);
        final HttpClient client = new HttpClient();
        client.setSslContextFactory(factory);
        return client;
    }

    @Override
    HttpServerOptions newHttpServerOptions() {
        return super.newHttpServerOptions()
            .setSsl(true)
            .setKeyStoreOptions(
                new JksOptions()
                    .setPath(
                        new TestResource("keystore").asPath().toString()
                    )
                    .setPassword("123456")
            );
    }
}
