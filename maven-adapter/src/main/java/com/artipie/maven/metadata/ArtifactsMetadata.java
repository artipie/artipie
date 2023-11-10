/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven.metadata;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.jcabi.xml.XMLDocument;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Read information from metadata file.
 * @since 0.5
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
                content -> new PublisherAs(content).string(StandardCharsets.UTF_8)
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
            .thenCompose(
                content -> new PublisherAs(content).string(StandardCharsets.UTF_8)
                    .thenApply(XMLDocument::new)
                    .thenApply(
                        doc -> new ImmutablePair<>(
                            doc.xpath("//groupId/text()").get(0),
                            doc.xpath("//artifactId/text()").get(0)
                        )
                    )
            );
    }
}
