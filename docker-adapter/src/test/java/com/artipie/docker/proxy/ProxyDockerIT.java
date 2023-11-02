/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.Catalog;
import com.artipie.http.client.Settings;
import com.artipie.http.client.jetty.JettyClientSlices;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsAnything;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.StringIsJson;

/**
 * Integration tests for {@link ProxyDocker}.
 *
 * @since 0.10
 */
final class ProxyDockerIT {

    /**
     * HTTP client used for proxy.
     */
    private JettyClientSlices client;

    /**
     * Proxy docker.
     */
    private ProxyDocker docker;

    @BeforeEach
    void setUp() throws Exception {
        this.client = new JettyClientSlices(new Settings.WithFollowRedirects(true));
        this.client.start();
        this.docker = new ProxyDocker(this.client.https("mcr.microsoft.com"));
    }

    @AfterEach
    void tearDown() throws Exception {
        this.client.stop();
    }

    @Test
    void readsCatalog() {
        MatcherAssert.assertThat(
            this.docker.catalog(Optional.empty(), Integer.MAX_VALUE)
                .thenApply(Catalog::json)
                .thenApply(PublisherAs::new)
                .thenCompose(PublisherAs::asciiString)
                .toCompletableFuture().join(),
            new StringIsJson.Object(new JsonHas("repositories", new IsAnything<>()))
        );
    }
}
