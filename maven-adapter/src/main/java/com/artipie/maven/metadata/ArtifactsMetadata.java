/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.metadata;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.jcabi.xml.XMLDocument;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.concurrent.CompletionStage;

/**
 * Read information from metadata file.
 */
public final class ArtifactsMetadata {

    /**
     * Maven metadata xml name.
     */
    public static final String MAVEN_METADATA = "maven-metadata.xml";

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    public ArtifactsMetadata(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Reads release version from maven-metadata.xml.
     * @param location Package location
     * @return Version as completed stage
     */
    public CompletionStage<String> maxVersion(final Key location) {
        return this.storage.value(new Key.From(location, ArtifactsMetadata.MAVEN_METADATA))
            .thenCompose(
                content -> content.asStringFuture()
                .thenApply(
                    metadata -> new XMLDocument(metadata).xpath("//version/text()").stream()
                        .max(Comparator.comparing(Version::new)).orElseThrow(
                            () -> new IllegalArgumentException(
                                "Maven metadata xml not valid: latest version not found"
                            )
                        )
                )
            );
    }

    /**
     * Reads group id and  artifact id from maven-metadata.xml.
     * @param location Package location
     * @return Pair of group id and artifact id
     */
    public CompletionStage<Pair<String, String>> groupAndArtifact(final Key location) {
        return this.storage.value(new Key.From(location, ArtifactsMetadata.MAVEN_METADATA))
            .thenCompose(Content::asStringFuture)
            .thenApply(val -> {
                XMLDocument doc = new XMLDocument(val);
                return new ImmutablePair<>(
                    doc.xpath("//groupId/text()").get(0),
                    doc.xpath("//artifactId/text()").get(0)
                );
            });
    }
}
