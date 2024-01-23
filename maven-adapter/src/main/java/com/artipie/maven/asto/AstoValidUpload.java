/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.maven.ValidUpload;
import com.artipie.maven.http.MavenSlice;
import com.artipie.maven.http.PutMetadataSlice;
import com.artipie.maven.metadata.ArtifactsMetadata;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Asto {@link ValidUpload} implementation validates upload from abstract storage. Validation
 * process includes:
 * - check maven-metadata.xml: group and artifact ids from uploaded xml are the same as in
 *  existing xml, checksums (if there are any) are valid
 * - artifacts checksums are correct
 * Upload contains all the uploaded artifacts, snapshot metadata and package metadata, here is the
 * example of upload layout:
 * <pre>
 * .upload/com/example/logger/0.1-SNAPSHOT
 * |  logger-0.1.jar
 * |  logger-0.1.jar.sha1
 * |  logger-0.1.jar.md5
 * |  logger-0.1.pom
 * |  logger-0.1.pom.sha1
 * |  logger-0.1.pom.md5
 * |  maven-metadata.xml          # snapshot metadata
 * |  maven-metadata.xml.sha1
 * |  maven-metadata.xml.md5
 * |--meta
 *    |  maven-metadata.xml       # package metadata
 *    |  maven-metadata.xml.sha1
 *    |  maven-metadata.xml.md5
 * </pre>
 * @since 0.5
 */
public final class AstoValidUpload implements ValidUpload {

    /**
     * All supported Maven artifacts according to
     * <a href="https://maven.apache.org/ref/3.6.3/maven-core/artifact-handlers.html">Artifact
     * handlers</a> by maven-core, and additionally {@code xml} metadata files are
     * also artifacts.
     */
    private static final Pattern PTN_ARTIFACT = Pattern.compile(
        String.format(".+\\.(?:%s)", String.join("|", MavenSlice.EXT))
    );

    /**
     * Maven metadata and metadata checksums.
     */
    private static final Pattern PTN_META =
        Pattern.compile(".+/meta/maven-metadata.xml.(?:md5|sha1|sha256|sha512)");

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Abstract storage
     */
    public AstoValidUpload(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Boolean> validate(final Key upload, final Key artifact) {
        return this.validateMetadata(upload, artifact)
            .thenCompose(
                valid -> {
                    CompletionStage<Boolean> res = CompletableFuture.completedStage(valid);
                    if (valid) {
                        res = this.validateChecksums(upload);
                    }
                    return res;
                }
            );
    }

    @Override
    public CompletionStage<Boolean> ready(final Key location) {
        return this.storage.list(location).thenApply(
            list -> list.stream().map(Key::string).collect(Collectors.toList())
        ).thenApply(
            list ->
                list.stream().filter(
                    key -> AstoValidUpload.PTN_ARTIFACT.matcher(key).matches()
                ).findAny().map(
                    item -> list.stream().filter(
                        key -> key.contains(item) && key.length() > item.length()
                    ).map(key -> key.substring(key.lastIndexOf('.'))).collect(Collectors.toList())
                ).map(
                    algs -> list.stream().filter(item -> PTN_META.matcher(item).matches())
                        .map(key -> key.substring(key.lastIndexOf('.')))
                        .collect(Collectors.toList()).equals(algs)
                ).orElse(false)
        );
    }

    /**
     * Validates uploaded and existing metadata by comparing group and artifact ids.
     * @param upload Uploaded artifacts location
     * @param artifact Artifact location
     * @return Completable validation action: true if group and artifact ids are equal,
     *  false otherwise.
     */
    private CompletionStage<Boolean> validateMetadata(final Key upload, final Key artifact) {
        final ArtifactsMetadata metadata = new ArtifactsMetadata(this.storage);
        final String meta = "maven-metadata.xml";
        return this.storage.exists(new Key.From(artifact, meta))
            .thenCompose(
                exists -> {
                    final CompletionStage<Boolean> res;
                    if (exists) {
                        res = metadata.groupAndArtifact(
                            new Key.From(upload, PutMetadataSlice.SUB_META)
                        ).thenCompose(
                            existing -> metadata.groupAndArtifact(artifact).thenApply(
                                uploaded -> uploaded.equals(existing)
                            )
                        );
                    } else {
                        res = CompletableFuture.completedStage(true);
                    }
                    return res;
                }
            ).thenCompose(
                same -> {
                    final CompletionStage<Boolean> res;
                    if (same) {
                        res = this.validateArtifactChecksums(
                            new Key.From(upload, meta)
                        ).to(SingleInterop.get());
                    } else {
                        res = CompletableFuture.completedStage(false);
                    }
                    return res;
                }
            );
    }

    /**
     * Validate artifact checksums.
     * @param upload Artifact location
     * @return Completable validation action: true if checksums are correct, false otherwise
     */
    private CompletionStage<Boolean> validateChecksums(final Key upload) {
        return new RxStorageWrapper(this.storage).list(upload)
            .flatMapObservable(Observable::fromIterable)
            .filter(key -> PTN_ARTIFACT.matcher(key.string()).matches())
            .flatMapSingle(
                this::validateArtifactChecksums
            ).reduce(
                new ArrayList<>(5),
                (list, res) -> {
                    list.add(res);
                    return list;
                }
            ).map(
                array -> !array.isEmpty() && !array.contains(false)
            ).to(SingleInterop.get());
    }

    /**
     * Validates artifact checksums.
     * @param artifact Artifact key
     * @return Validation result: false if at least one checksum is invalid, true if all are valid
     *  or if no checksums exists.
     */
    private Single<Boolean> validateArtifactChecksums(final Key artifact) {
        return SingleInterop.fromFuture(
            new RepositoryChecksums(this.storage).checksums(artifact)
        ).map(Map::entrySet)
            .flatMapObservable(Observable::fromIterable)
            .flatMapSingle(
                entry ->
                    SingleInterop.fromFuture(
                        this.storage.value(artifact).thenCompose(
                            content -> new ContentDigest(
                                content,
                                Digests.valueOf(
                                    entry.getKey().toUpperCase(Locale.US)
                                )
                            ).hex().thenApply(
                                hex -> hex.equals(entry.getValue())
                            )
                    )
                )
            ).all(equal -> equal);
    }
}
