/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven.asto;

import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.maven.Maven;
import com.artipie.maven.http.PutMetadataSlice;
import com.artipie.maven.metadata.MavenMetadata;
import com.jcabi.xml.XMLDocument;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.xembly.Directives;

/**
 * Maven front for artipie maven adaptor.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AstoMaven implements Maven {

    /**
     * Maven metadata xml name.
     */
    private static final String MAVEN_META = "maven-metadata.xml";

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * Constructor.
     * @param storage Storage used by this class.
     */
    public AstoMaven(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Void> update(final Key upload, final Key artifact) {
        return this.storage.exclusively(
            artifact,
            target -> target.list(artifact).thenApply(
                items -> items.stream()
                    .map(
                        item -> item.string()
                            .replaceAll(String.format("%s/", artifact.string()), "")
                            .split("/")[0]
                    )
                    .filter(item -> !item.startsWith("maven-metadata"))
                    .collect(Collectors.toSet())
            ).thenCompose(
                versions ->
                    this.storage.value(
                        new Key.From(upload, PutMetadataSlice.SUB_META, AstoMaven.MAVEN_META)
                    ).thenCompose(pub -> new PublisherAs(pub).asciiString())
                        .thenCompose(
                            str -> {
                                versions.add(new KeyLastPart(upload).get());
                                return new MavenMetadata(
                                    Directives.copyOf(new XMLDocument(str).node())
                                ).versions(versions).save(
                                    this.storage, new Key.From(upload, PutMetadataSlice.SUB_META)
                                );
                            }
                        )
            )
                .thenCompose(meta -> new RepositoryChecksums(this.storage).generate(meta))
                .thenCompose(nothing -> this.moveToTheRepository(upload, target, artifact))
                .thenCompose(nothing -> this.storage.deleteAll(upload))
            );
    }

    /**
     * Moves artifacts from temp location to repository.
     * @param upload Upload temp location
     * @param target Repository
     * @param artifact Artifact repository location
     * @return Completion action
     */
    private CompletableFuture<Void> moveToTheRepository(
        final Key upload, final Storage target, final Key artifact
    ) {
        final Storage sub = new SubStorage(
            new Key.From(upload, PutMetadataSlice.SUB_META), this.storage
        );
        final Storage subversion = new SubStorage(upload.parent().get(), this.storage);
        return sub.list(Key.ROOT).thenCompose(
            list -> new Copy(
                sub,
                list.stream().filter(key -> key.string().contains(AstoMaven.MAVEN_META))
                    .collect(Collectors.toList())
            ).copy(new SubStorage(artifact, target))
        ).thenCompose(
            nothing -> subversion.list(Key.ROOT).thenCompose(
                list -> new Copy(
                    subversion,
                    list.stream()
                        .filter(
                            key -> !key.string().contains(
                                String.format("/%s/", PutMetadataSlice.SUB_META)
                            )
                        ).collect(Collectors.toList())
                ).copy(new SubStorage(artifact, target))
            )
        );
    }
}
