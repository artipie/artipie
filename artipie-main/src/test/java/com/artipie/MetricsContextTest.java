/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
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
            "Metrics (all types) are enabled",
            metrics.enabled() && metrics.jvm() && metrics.http() && metrics.storage()
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
            "Metrics (all types) are disabled",
            !metrics.enabled() && !metrics.jvm() && !metrics.http() && !metrics.storage()
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

    @Test
    void returnsTrueWhenTypeIsEnabled() {
        final MetricsContext metrics =
            new MetricsContext(this.settings("/any", 9876, "jvm", "http", "storage"));
        MatcherAssert.assertThat(
            "Metrics (all types) are enabled",
            metrics.jvm() && metrics.http() && metrics.storage()
        );
    }

    @Test
    void returnsFalseWhenTypeIsDisabled() {
        final MetricsContext metrics =
            new MetricsContext(this.settings("/any", 9876));
        MatcherAssert.assertThat(
            "Metrics (all types) are disabled",
            !metrics.jvm() && !metrics.http() && !metrics.storage()
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

    private YamlMapping settings(
        final String endpoint, final int port, final String... types
    ) {
        final YamlMappingBuilder res = Yaml.createYamlMappingBuilder()
            .add("port", String.valueOf(port))
            .add("endpoint", endpoint);
        YamlSequenceBuilder seq = Yaml.createYamlSequenceBuilder();
        for (final String item : types) {
            seq = seq.add(item);
        }
        return Yaml.createYamlMappingBuilder()
            .add("metrics", res.add("types", seq.build()).build()).build();
    }
}
