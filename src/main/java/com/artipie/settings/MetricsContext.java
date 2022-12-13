/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Metrics context.
 *
 * @since 0.28.0
 */
public final class MetricsContext {

    /**
     * Endpoint for metrics.
     */
    private static final String ENDPOINT = "endpoint";

    /**
     * Port for metrics.
     */
    private static final String PORT = "port";

    /**
     * Metrics yaml section.
     */
    private static final String METRICS = "metrics";

    /**
     * Meta section from Artipie yaml settings.
     */
    private final YamlMapping meta;

    /**
     * Ctor.
     * @param meta Meta section from Artipie yaml settings
     */
    public MetricsContext(final YamlMapping meta) {
        this.meta = meta;
    }

    /**
     * Are metrics enabled?
     * @return True if metrics are enabled
     */
    public boolean enabled() {
        return this.endpointAndPort().isPresent();
    }

    /**
     * Get endpoint and port for the metrics Prometheus endpoint.
     * @return Endpoint and port is present
     */
    public Optional<Pair<String, Integer>> endpointAndPort() {
        Optional<Pair<String, Integer>> res = Optional.empty();
        final YamlMapping metrics = this.meta.yamlMapping(MetricsContext.METRICS);
        if (metrics != null && metrics.string(MetricsContext.ENDPOINT) != null
            && metrics.value(MetricsContext.PORT) != null) {
            res = Optional.of(
                new ImmutablePair<>(
                    metrics.string(MetricsContext.ENDPOINT), metrics.integer(MetricsContext.PORT)
                )
            );
        }
        return res;
    }

    /**
     * Are JVM metrics enabled?
     * @return True is yes
     */
    public boolean jvm() {
        return this.enabled() && this.isTypeEnabled("jvm");
    }

    /**
     * Are storage metrics enabled?
     * @return True is yes
     */
    public boolean storage() {
        return this.enabled() && this.isTypeEnabled("storage");
    }

    /**
     * Are http (requests) metrics enabled?
     * @return True is yes
     */
    public boolean http() {
        return this.enabled() && this.isTypeEnabled("http");
    }

    /**
     * Check if given metrics type is enabled.
     * @param type Type to check
     * @return True is enabled
     */
    private boolean isTypeEnabled(final String type) {
        boolean res = true;
        final YamlSequence types = this.meta.yamlMapping(MetricsContext.METRICS)
            .yamlSequence("types");
        if (types != null) {
            res = types.values().stream().map(node -> node.asScalar().value())
                .anyMatch(val -> val.equals(type));
        }
        return res;
    }

}
