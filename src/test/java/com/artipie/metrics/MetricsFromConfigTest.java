/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
