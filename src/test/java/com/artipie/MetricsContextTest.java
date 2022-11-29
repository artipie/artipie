/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.settings.MetricsContext;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for Metrics context.
 *
 * @since 0.28.0
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class MetricsContextTest {

    @Test
    void readsPortAndEndpoint() {
        final int port = 8080;
        final String endpoint = "/metrics/prometheus";
        final MetricsContext metrics = new MetricsContext(
            this.settings(Optional.of(endpoint), Optional.of(port))
        );
        MatcherAssert.assertThat(
            "Returns port and endpoint",
            metrics.endpointAndPort().get(),
            new IsEqual<>(new ImmutablePair<>(endpoint, port))
        );
        MatcherAssert.assertThat(
            "Metrics are enabled",
            metrics.enabled()
        );
    }

    @Test
    void returnsEmptyIfEndpointIsAbsent() {
        final MetricsContext metrics =
            new MetricsContext(this.settings(Optional.empty(), Optional.of(987)));
        MatcherAssert.assertThat(
            "Endpoint and port are empty",
            metrics.endpointAndPort().isEmpty(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Metrics are disabled",
            !metrics.enabled()
        );
    }

    @Test
    void returnsEmptyIfPortIsAbsent() {
        final MetricsContext metrics =
            new MetricsContext(this.settings(Optional.of("/any"), Optional.empty()));
        MatcherAssert.assertThat(
            "Endpoint and port are empty",
            metrics.endpointAndPort().isEmpty(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Metrics are disabled",
            !metrics.enabled()
        );
    }

    private YamlMapping settings(final Optional<String> endpoint, final Optional<Integer> port) {
        YamlMappingBuilder res = Yaml.createYamlMappingBuilder();
        if (port.isPresent()) {
            res = res.add("port", String.valueOf(port.get()));
        }
        if (endpoint.isPresent()) {
            res = res.add("endpoint", endpoint.get());
        }
        return Yaml.createYamlMappingBuilder().add("metrics", res.build()).build();
    }
}
