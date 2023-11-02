/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.misc;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Differences of packages, listed in primary and packages in the repository.
 * @since 1.10
 */
public final class PackagesDiff {

    /**
     * Packages, written in primary xml.
     * Packages file name &lt;-&gt; checksum map.
     */
    private final Map<String, String> primary;

    /**
     * Packages, located in the repository.
     * Packages file name &lt;-&gt; checksum map.
     */
    private final Map<String, String> repo;

    /**
     * Ctor.
     * @param first First map
     * @param second Second map
     */
    public PackagesDiff(final Map<String, String> first, final Map<String, String> second) {
        this.primary = first;
        this.repo = second;
    }

    /**
     * Packages that should be removed from the repo.
     * @return Package name &lt;-&gt; checksum
     */
    public Map<String, String> toDelete() {
        return Maps.difference(this.primary, this.repo).entriesOnlyOnLeft();
    }

    /**
     * Return packages, that should be added/updated in the repository. These packages are:<br/>
     * 1) packages, that are present in repo and not present in primary<br/>
     * 2) packages, that are present both in repo and primary, but have different checksums
     * @return Collection with packages names
     */
    public Collection<String> toAdd() {
        return Stream.concat(
            Maps.difference(this.primary, this.repo).entriesDiffering().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().leftValue()))
                .entrySet().stream(),
            Maps.difference(this.primary, this.repo).entriesOnlyOnRight().entrySet().stream()
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).keySet();
    }
}
