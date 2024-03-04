/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.Catalog;
import com.artipie.http.client.HttpClientSettings;
import com.artipie.http.client.jetty.JettyClientSlices;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsAnything;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.StringIsJson;

import java.util.Optional;

/**
 * Integration tests for {@link ProxyDocker}.
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
    void setUp() {
        this.client = new JettyClientSlices(
            new HttpClientSettings().setFollowRedirects(true)
        );
        this.client.start();
        this.docker = new ProxyDocker(this.client.https("mcr.microsoft.com"));
    }

    @AfterEach
    void tearDown() {
        this.client.stop();
    }

    @Test
    void readsCatalog() {
        MatcherAssert.assertThat(
            this.docker.catalog(Optional.empty(), Integer.MAX_VALUE)
                .thenApply(Catalog::json)
                .toCompletableFuture().join().asString(),
            new StringIsJson.Object(new JsonHas("repositories", new IsAnything<>()))
        );
    }
}
