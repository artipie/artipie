/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
     * Jvm metrics type.
     */
    private static final String TYPE_JVM = "jvm";

    /**
     * Http metrics type.
     */
    private static final String TYPE_HTTP = "http";

    /**
     * Storage metrics type.
     */
    private static final String TYPE_STORAGE = "storage";

    /**
     * Meta section from Artipie yaml settings.
     */
    private final Optional<Pair<String, Integer>> pair;

    /**
     * Enabled metrics types.
     */
    private final Set<String> types;

    /**
     * Ctor.
     * @param meta Meta section from Artipie yaml settings
     */
    public MetricsContext(final YamlMapping meta) {
        this.pair = MetricsContext.parseYaml(meta);
        this.types = Optional.ofNullable(meta.yamlMapping(MetricsContext.METRICS))
            .flatMap(map -> Optional.ofNullable(map.yamlSequence("types")))
            .map(
                seq -> seq.values().stream()
                    .map(item -> item.asScalar().value()).collect(Collectors.toSet())
            )
            .orElse(
                Set.of(
                    MetricsContext.TYPE_HTTP, MetricsContext.TYPE_JVM, MetricsContext.TYPE_STORAGE
                )
            );
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
        return this.pair;
    }

    /**
     * Are JVM metrics enabled?
     * @return True is yes
     */
    public boolean jvm() {
        return this.enabled() && this.types.contains(MetricsContext.TYPE_JVM);
    }

    /**
     * Are storage metrics enabled?
     * @return True is yes
     */
    public boolean storage() {
        return this.enabled() && this.types.contains(MetricsContext.TYPE_STORAGE);
    }

    /**
     * Are http (requests) metrics enabled?
     * @return True is yes
     */
    public boolean http() {
        return this.enabled() && this.types.contains(MetricsContext.TYPE_HTTP);
    }

    /**
     * Get endpoint and port pair from yaml.
     * @param meta Yaml mapping
     * @return Endpoint and port pair if present
     */
    private static Optional<Pair<String, Integer>> parseYaml(final YamlMapping meta) {
        Optional<Pair<String, Integer>> res = Optional.empty();
        final YamlMapping metrics = meta.yamlMapping(MetricsContext.METRICS);
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
