/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.metadata;

import com.jcabi.xml.XMLDocument;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extracts information from the metadata maven client sends on deploy.
 * @since 0.8
 */
public final class DeployMetadata {

    /**
     * Metadata.
     */
    private final String data;

    /**
     * Ctor.
     * @param data Metadata
     */
    public DeployMetadata(final String data) {
        this.data = data;
    }

    /**
     * Get versioning/release tag value.
     * @return Completion action
     */
    public String release() {
        final List<String> release = new XMLDocument(this.data).xpath("//release/text()");
        if (release.isEmpty()) {
            throw new IllegalArgumentException("Failed to read deploy maven metadata");
        } else {
            return release.get(0);
        }
    }

    /**
     * Reads snapshot versions from metadata.xml.
     * @return List of snapshot versions
     */
    public Set<String> snapshots() {
        return new XMLDocument(this.data).xpath("//version/text()").stream()
            .filter(item -> item.contains("SNAPSHOT")).collect(Collectors.toSet());
    }
}
