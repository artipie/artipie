/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.headers.Accept;
import com.artipie.http.rs.RsStatus;
import io.vertx.reactivex.core.Vertx;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * IT tests for Prometheus metrics.
 * @since 0.23
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class PromuSliceITCase {

    @Test
    void givesAccessWhenPrometheusConfigIsAvailable(
        @TempDir final Path root) throws Exception {
        final int port = runServer(
            root,
            Yaml.createYamlMappingBuilder()
                .add(
                    "metrics",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "prometheus")
                        .build()
                )
        );
        final HttpURLConnection con = (HttpURLConnection) new URL(formatUrl(port))
            .openConnection();
        con.setRequestMethod(HttpMethod.GET);
        con.setRequestProperty(Accept.NAME, MediaType.TEXT_PLAIN);
        MatcherAssert.assertThat(
            con.getResponseCode(),
            new IsEqual<>(
                Integer.parseInt(RsStatus.OK.code())
            )
        );
        con.disconnect();
    }

    @Test
    void blocksAccessWhenPrometheusConfigIsUnavailable(
        @TempDir final Path root) throws Exception {
        final int port = runServer(
            root,
            Yaml.createYamlMappingBuilder()
        );
        final HttpURLConnection con = (HttpURLConnection) new URL(formatUrl(port))
            .openConnection();
        con.setRequestMethod(HttpMethod.GET);
        con.setRequestProperty(Accept.NAME, MediaType.TEXT_PLAIN);
        MatcherAssert.assertThat(
            con.getResponseCode(),
            new IsEqual<>(
                Integer.parseInt(RsStatus.NOT_FOUND.code())
            )
        );
        con.disconnect();
    }

    /**
     * Runs an Artipie server.
     * @param root Server root folder
     * @param meta Meta config
     * @return Port on which the server starts
     */
    private static int runServer(
        final Path root, final YamlMappingBuilder meta) throws Exception {
        final Vertx vertx = Vertx.vertx();
        final Path repos = root.resolve("repos");
        repos.toFile().mkdir();
        final Path cfg = root.resolve("artipie.yaml");
        Files.write(
            cfg,
            Yaml.createYamlMappingBuilder()
                .add(
                    "meta",
                    meta.add(
                        "storage",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "fs")
                            .add("path", repos.toString())
                            .build()
                    ).build()
                )
                .build().toString().getBytes()
        );
        final JettyClientSlices http = new JettyClientSlices(new HttpClientSettings());
        http.start();
        final VertxMain server = new VertxMain(http, cfg, vertx, 0);
        return server.start();
    }

    /**
     * Gives Prometheus metrics url.
     * @param port Port
     * @return Prometheus metrics url
     */
    private static String formatUrl(final int port) {
        return String.format("http://localhost:%s/prometheus/metrics", port);
    }
}
