/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * IT tests for Prometheus metrics.
 * @since 0.23
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class PrometheusSliceITCase {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        new MapOf<>(
            new MapEntry<>(
                "simple-artipie",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
            ),
            new MapEntry<>(
                "artipie-prometheus",
                () -> TestDeployment.ArtipieContainer.defaultDefinition()
                    .withConfig("artipie-metrics-prometheus.yaml")
            )
        ),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.containers.assertExec(
            "Failed to install deps",
            new ContainerResultMatcher(),
            "apk", "add", "--no-cache", "curl"
        );
    }

    @Test
    void givesAccessWhenPrometheusConfigIsAvailable() throws Exception {
        this.containers.assertExec(
            "Failed to download artifact",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS,
                new StringContains("HTTP/1.1 200 OK")
            ),
            "curl", "-i", "-X", "GET", "http://artipie-prometheus:8080/prometheus/metrics"
        );
    }

    @Test
    void blocksAccessWhenPrometheusConfigIsUnavailable() throws Exception {
        this.containers.assertExec(
            "Failed to download artifact",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS,
                new StringContains("HTTP/1.1 404 Not Found")
            ),
            "curl", "-i", "-X", "GET", "http://simple-artipie:8080/prometheus/metrics"
        );
    }

}
