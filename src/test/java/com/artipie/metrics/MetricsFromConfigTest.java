/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.metrics.memory.InMemoryMetrics;
import java.time.temporal.ChronoUnit;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Test for {@link MetricsFromConfig}.
 *
 * @since 0.9
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MetricsFromConfigTest {

    @Test
    void failsIfTypeIsNotSpecified() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new MetricsFromConfig(
                    Yaml.createYamlMappingBuilder().add("three", "four").build()
                ).metrics()
            ).getMessage(),
            new StringContains("Metrics type is not specified")
        );
    }

    @Test
    void failsIfTypeIsUnsupported() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new MetricsFromConfig(
                    Yaml.createYamlMappingBuilder().add("type", "any").build()
                ).metrics()
            ).getMessage(),
            new StringContains("Unsupported metrics type")
        );
    }

    @Test
    void parsesSettings() {
        MatcherAssert.assertThat(
            new MetricsFromConfig(
                Yaml.createYamlMappingBuilder().add("type", "log").add("interval", "10").build()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super MetricsFromConfig>>(
                    new MatcherOf<>(metrics -> metrics.metrics() instanceof InMemoryMetrics),
                    new MatcherOf<>(metrics -> metrics.interval().get(ChronoUnit.SECONDS) == 10)
                )
            )
        );
    }

    @Test
    void usesDefaultInterval() {
        MatcherAssert.assertThat(
            new MetricsFromConfig(
                Yaml.createYamlMappingBuilder().add("type", "log").build()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super MetricsFromConfig>>(
                    new MatcherOf<>(metrics -> metrics.metrics() instanceof InMemoryMetrics),
                    new MatcherOf<>(metrics -> metrics.interval().get(ChronoUnit.SECONDS) == 5)
                )
            )
        );
    }

}
