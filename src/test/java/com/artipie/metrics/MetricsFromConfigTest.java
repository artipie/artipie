/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.metrics.memory.InMemoryMetrics;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MetricsFromConfig}.
 *
 * @since 0.9
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MetricsFromConfigTest {

    @Test
    void parsesSettings() {
        MatcherAssert.assertThat(
            new MetricsFromConfig(
                Yaml.createYamlSequenceBuilder().add(
                    Yaml.createYamlMappingBuilder().add("type", "log").add("interval", "10").build()
                ).build()
            ).metrics(),
            new IsInstanceOf(InMemoryMetrics.class)
        );
    }

    @Test
    void readsVertxSettings() {
        final String path = "/metrics/vertx";
        final int port = 8083;
        MatcherAssert.assertThat(
            new MetricsFromConfig(
                Yaml.createYamlSequenceBuilder().add(
                    Yaml.createYamlMappingBuilder()
                        .add("type", "vertx")
                        .add("path", path)
                        .add("port", String.valueOf(port)).build()
                ).build()
            ).vertxMetricsConf().get(),
            new IsEqual<>(new ImmutablePair<>(path, port))
        );
    }

}
