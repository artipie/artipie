/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tags;
import com.artipie.http.client.Settings;
import com.artipie.http.client.jetty.JettyClientSlices;
import java.util.Optional;
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
 *
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class ProxyManifestsIT {

    /**
     * HTTP client used for proxy.
     */
    private JettyClientSlices client;

    @BeforeEach
    void setUp() throws Exception {
        this.client = new JettyClientSlices(new Settings.WithFollowRedirects(true));
        this.client.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        this.client.stop();
    }

    @Test
    void readsTags() {
        final String repo = "dotnet/runtime";
        MatcherAssert.assertThat(
            new ProxyManifests(
                this.client.https("mcr.microsoft.com"),
                new RepoName.Simple(repo)
            ).tags(Optional.empty(), Integer.MAX_VALUE)
                .thenApply(Tags::json)
                .thenApply(PublisherAs::new)
                .thenCompose(PublisherAs::asciiString)
                .toCompletableFuture().join(),
            new StringIsJson.Object(
                Matchers.allOf(
                    new JsonHas("name", new JsonValueIs(repo)),
                    new JsonHas("tags", new IsAnything<>())
                )
            )
        );
    }
}
