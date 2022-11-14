/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.metrics.publish.MetricsOutputType;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for Metrics context.
 *
 * @since 0.28.0
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class MetricsContextTest {

    /**
     * Metrics context.
     */
    @SuppressWarnings("PMD.ImmutableField")
    private MetricsContext ctx = new MetricsContext();

    @Test
    void metricsDisabledWhenMetricsFieldIsAbsent() {
        this.ctx.init(
            Yaml.createYamlMappingBuilder().build()
        );
        this.assertAllMetrics(false);
    }

    @Test
    void metricsDisabledWhenMetricsFieldIsEmpty() {
        this.ctx.init(
            Yaml.createYamlMappingBuilder()
                .add(
                    "metrics",
                    Yaml.createYamlSequenceBuilder().build()
                ).build()
        );
        this.assertAllMetrics(false);
    }

    @Test
    void parsesSettings() {
        this.ctx.init(
            Yaml.createYamlMappingBuilder()
                .add(
                    "metrics",
                    Yaml.createYamlSequenceBuilder()
                        .add(
                            Yaml.createYamlMappingBuilder()
                                .add("type", "asto")
                                .add("interval", "10")
                                .add(
                                    "storage",
                                    Yaml.createYamlMappingBuilder()
                                        .add("type", "fs")
                                        .add("path", "/tmp/artipie/statistict")
                                        .build()
                                ).build()
                        )
                        .add(
                            Yaml.createYamlMappingBuilder()
                                .add("type", "prometheus")
                                .build()
                        )
                        .add(
                            Yaml.createYamlMappingBuilder()
                                .add("type", "log")
                                .build()
                        )
                        .add(
                            Yaml.createYamlMappingBuilder()
                                .add("type", "vertx")
                                .add("endpoint", "test_endpoint")
                                .add("port", "333")
                                .build()
                        )
                        .build()
                ).build()
        );
        this.assertAllMetrics(true);
        MatcherAssert.assertThat(this.ctx.vertxEndpoint(), Is.is("test_endpoint"));
        MatcherAssert.assertThat(this.ctx.vertxPort(), Is.is(333));
    }

    @Test
    void initShouldThrowExceptionWhenMetricsTypeIsUnknown() {
        try {
            this.ctx.init(
                Yaml.createYamlMappingBuilder()
                    .add(
                        "metrics",
                        Yaml.createYamlSequenceBuilder()
                            .add(
                                Yaml.createYamlMappingBuilder()
                                    .add("type", "test_fail")
                                    .build()
                            )
                            .build()
                    ).build()
            );
            Assertions.fail();
        } catch (final IllegalArgumentException err) {
            MatcherAssert.assertThat(
                err.getMessage(),
                StringContains.containsString(
                    "No enum constant com.artipie.metrics.publish.MetricsOutputType.TEST_FAIL"
                )
            );
        }
    }

    @Test
    void initShouldThrowExceptionWhenMetricsTypeIsEmpty() {
        try {
            this.ctx.init(
                Yaml.createYamlMappingBuilder()
                    .add(
                        "metrics",
                        Yaml.createYamlSequenceBuilder()
                            .add(
                                Yaml.createYamlMappingBuilder()
                                    .add("type", "")
                                    .build()
                            )
                            .build()
                    ).build()
            );
            Assertions.fail();
        } catch (final IllegalArgumentException err) {
            MatcherAssert.assertThat(
                err.getMessage(),
                StringContains.containsString(
                    "Empty metric type is not allowed"
                )
            );
        }
    }

    @Test
    void metricsShouldThrowExceptionWhenMetricsAreNotEnabled() {
        try {
            this.ctx.init(
                Yaml.createYamlMappingBuilder().build()
            );
            this.ctx.getMetrics();
            Assertions.fail();
        } catch (final IllegalStateException err) {
            MatcherAssert.assertThat(
                err.getMessage(),
                StringContains.containsString(
                    "Metrics are not defined"
                )
            );
        }
    }

    @Test
    void metricsStorageShouldThrowExceptionWhenStorageIsNotEnabled() {
        try {
            this.ctx.init(
                Yaml.createYamlMappingBuilder()
                    .add(
                        "metrics",
                        Yaml.createYamlSequenceBuilder()
                            .add(
                                Yaml.createYamlMappingBuilder()
                                    .add("type", "log")
                                    .build()
                            )
                            .build()
                    ).build()
            );
            this.ctx.metricsStorage();
            Assertions.fail();
        } catch (final IllegalStateException err) {
            MatcherAssert.assertThat(
                err.getMessage(),
                StringContains.containsString(
                    "Storage type metrics are not defined"
                )
            );
        }
    }

    /**
     * Checks that all metrics are disabled or enabled.
     *
     * @param enabled Enabled flag.
     */
    private void assertAllMetrics(final boolean enabled) {
        MatcherAssert.assertThat(this.ctx.enabled(), Is.is(enabled));
        MatcherAssert.assertThat(
            this.ctx.enabledMetricsOutput(MetricsOutputType.LOG), Is.is(enabled)
        );
        MatcherAssert.assertThat(
            this.ctx.enabledMetricsOutput(MetricsOutputType.ASTO), Is.is(enabled)
        );
        MatcherAssert.assertThat(
            this.ctx.enabledMetricsOutput(MetricsOutputType.PROMETHEUS), Is.is(enabled)
        );
        MatcherAssert.assertThat(
            this.ctx.enabledMetricsOutput(MetricsOutputType.VERTX), Is.is(enabled)
        );
    }
}
