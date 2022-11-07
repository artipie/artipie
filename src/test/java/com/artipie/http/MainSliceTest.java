/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.metrics.MetricsFromConfig;
import com.artipie.settings.Settings;
import com.artipie.settings.users.UsersFromEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * MainSlice tests.
 * @since 0.11
 */
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
