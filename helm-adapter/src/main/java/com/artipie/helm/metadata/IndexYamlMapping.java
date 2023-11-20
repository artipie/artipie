/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.metadata;

import com.artipie.asto.Content;
import com.artipie.helm.misc.DateTimeNow;
import com.artipie.helm.misc.EmptyIndex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Mapping for content from index.yaml file.
 * @todo #94:30min Residing this class in order to remove
 *  synchronized block, preferring keeping it immutable.
 * @since 0.2
 */
@SuppressWarnings("unchecked")
public final class IndexYamlMapping {
    /**
     * Entries.
     */
    private static final String ENTRS = "entries";

    /**
     * Version.
     */
    private static final String VRSN = "version";

    /**
     * Mapping for fields from index.yaml file.
     */
    private final Map<String, Object> mapping;

    /**
     * Ctor.
     */
    public IndexYamlMapping() {
        this(new EmptyIndex().asString());
    }

    /**
     * Ctor.
     * @param yaml Index.yaml file
     */
    public IndexYamlMapping(final String yaml) {
        this((Map<String, Object>) new Yaml().load(yaml));
    }

    /**
     * Ctor.
     * @param mapfromindex Mapping for fields from index.yaml file
     */
    public IndexYamlMapping(final Map<String, Object> mapfromindex) {
        this.mapping = mapfromindex;
    }

    /**
     * Obtain mapping for `entries`.
     * @return Mapping for `entries`.
     */
    public Map<String, Object> entries() {
        this.mapping.computeIfAbsent(IndexYamlMapping.ENTRS, k -> new HashMap<>());
        return (Map<String, Object>) this.mapping.get(IndexYamlMapping.ENTRS);
    }

    /**
     * Obtain mapping for specified chart from `entries`.
     * @param chartname Chart name
     * @return Mapping for specified chart from `entries`.
     */
    public List<Map<String, Object>> byChart(final String chartname) {
        this.entries().computeIfAbsent(chartname, nothing -> new ArrayList<Map<String, Object>>(0));
        return (List<Map<String, Object>>) this.entries().get(chartname);
    }

    /**
     * Obtains entry with specified version and chart name.
     * @param chartname Chart name
     * @param version Version of chart
     * @return Entry if version for specified name exists, empty otherwise.
     */
    public Optional<Map<String, Object>> byChartAndVersion(final String chartname,
        final String version) {
        return this.byChart(chartname).stream()
            .filter(entry -> entry.get(IndexYamlMapping.VRSN).equals(version))
            .findFirst();
    }

    /**
     * Add info about chart to the existing mapping.
     * @param name Chart name
     * @param versions Collection with mapping for different versions of specified chart
     */
    public void addChartVersions(
        final String name,
        final List<Map<String, Object>> versions
    ) {
        synchronized (this.mapping) {
            final Map<String, Object> entr = this.entries();
            versions.forEach(vers -> vers.put("created", new DateTimeNow().asString()));
            if (entr.containsKey(name)) {
                final List<Map<String, Object>> existed = this.byChart(name);
                for (final Map<String, Object> vers : versions) {
                    final Optional<Map<String, Object>> opt;
                    opt = this.byChartAndVersion(name, (String) vers.get(IndexYamlMapping.VRSN));
                    if (opt.isPresent()) {
                        existed.removeIf(
                            chart -> chart.get(IndexYamlMapping.VRSN)
                                .equals(opt.get().get(IndexYamlMapping.VRSN))
                        );
                    }
                    existed.add(vers);
                }
            } else {
                entr.put(name, versions);
            }
        }
    }

    /**
     * Converts mapping to bytes.
     * @return Bytes if entries mapping contains any chart, empty otherwise.
     */
    public Optional<Content> toContent() {
        final Optional<Content> res;
        if (this.entries().isEmpty()) {
            res = Optional.empty();
        } else {
            this.mapping.put("generated", new DateTimeNow().asString());
            res = Optional.of(new Content.From(this.toString().getBytes()));
        }
        return res;
    }

    @Override
    public String toString() {
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return new Yaml(options).dump(this.mapping);
    }
}
