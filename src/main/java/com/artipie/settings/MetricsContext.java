/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
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
     * Endpoint for metrics.
     */
    private static final String PORT = "port";

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
        final YamlMapping metrics = this.meta.yamlMapping("metrics");
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

}
