/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.Tags;
import com.artipie.docker.misc.Pagination;
import com.artipie.http.client.HttpClientSettings;
import com.artipie.http.client.jetty.JettyClientSlices;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsAnything;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

/**
 * Integration tests for {@link ProxyManifests}.
 */
final class ProxyManifestsIT {

    /**
     * HTTP client used for proxy.
     */
    private JettyClientSlices client;

    @BeforeEach
    void setUp() {
        this.client = new JettyClientSlices(
            new HttpClientSettings().setFollowRedirects(true)
        );
        this.client.start();
    }

    @AfterEach
    void tearDown() {
        this.client.stop();
    }

    @Test
    void readsTags() {
        final String repo = "dotnet/runtime";
        MatcherAssert.assertThat(
            new ProxyManifests(
                this.client.https("mcr.microsoft.com"),
                repo
            ).tags(Pagination.empty())
                .thenApply(Tags::json)
                .toCompletableFuture().join().asString(),
            new StringIsJson.Object(
                Matchers.allOf(
                    new JsonHas("name", new JsonValueIs(repo)),
                    new JsonHas("tags", new IsAnything<>())
                )
            )
        );
    }
}
