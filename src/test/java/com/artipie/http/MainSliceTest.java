/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.metrics.MetricsFromConfig;
import com.artipie.settings.Settings;
import com.artipie.settings.users.UsersFromEnv;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * MainSlice tests.
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class MainSliceTest {

    @Test
    public void isPrometheusConfigAvailableShouldReturnFalseWhenPrometheusIsNotDefined() {
        Assertions.assertFalse(
            MainSlice.isPrometheusConfigAvailable(
                MainSliceTest.createSettings("log")
            ));
    }

    @Test
    public void isPrometheusConfigAvailableShouldReturnFalseWhenPrometheusIsDefined() {
        Assertions.assertTrue(
            MainSlice.isPrometheusConfigAvailable(
                MainSliceTest.createSettings("asto", MetricsFromConfig.PROMETHEUS)
            ));
    }

    @Test
    public void isPrometheusConfigAvailableShouldReturnFalseWhenMetricsAreAbsent() {
        Assertions.assertFalse(
            MainSlice.isPrometheusConfigAvailable(
                new Settings.Fake(
                    new UsersFromEnv(),
                    Yaml.createYamlMappingBuilder()
                        .build()
                )
            )
        );
    }

    @Test
    public void metricsStorageShouldReturnYamlMappingOptionalWhenAstoDefined() {
        final Optional<YamlMapping> res = MainSlice.metricsStorage(
            new Settings.Fake(
                new UsersFromEnv(),
                Yaml.createYamlMappingBuilder()
                    .add(
                        "metrics",
                        Yaml.createYamlSequenceBuilder()
                            .add(
                                Yaml.createYamlMappingBuilder()
                                    .add("type", "asto")
                                    .add(
                                        "storage",
                                        Yaml.createYamlMappingBuilder()
                                            .add("type", "fs")
                                            .add("path", "/tmp/artipie/statistict")
                                            .build()
                                    ).build()
                            )
                            .build()
                    ).build()
            )
        );
        Assertions.assertTrue(res.isPresent());
        MatcherAssert.assertThat(res.get().string("type"), Is.is("fs"));
        MatcherAssert.assertThat(
            res.get().string("path"),
            Is.is("/tmp/artipie/statistict")
        );
    }

    @Test
    public void metricsStorageShouldReturnEmptyOptionalWhenTypeIsNotAsto() {
        Assertions.assertFalse(
            MainSlice.metricsStorage(
                createSettings("log")
            ).isPresent()
        );
    }

    /**
     * Creates settings.
     *
     * @param metrics Array of metric types.
     * @return Settings.
     */
    private static Settings createSettings(final String... metrics) {
        YamlSequenceBuilder seq = Yaml.createYamlSequenceBuilder();
        for (final String type : metrics) {
            seq = seq.add(
                Yaml.createYamlMappingBuilder()
                    .add("type", type).build()
            );
        }
        return new Settings.Fake(
            new UsersFromEnv(),
            Yaml.createYamlMappingBuilder()
                .add(
                    "metrics",
                    seq.build()
                ).build()
        );
    }

}
