/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.asto.misc.UncheckedScalar;
import com.artipie.asto.streams.StorageValuePipeline;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.meta.MergedXml;
import com.artipie.rpm.meta.MergedXmlPackage;
import com.artipie.rpm.meta.MergedXmlPrimary;
import com.artipie.rpm.meta.XmlAlter;
import com.artipie.rpm.meta.XmlEvent;
import com.artipie.rpm.meta.XmlEventPrimary;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.pkg.Package;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.zip.GZIPInputStream;

/**
 * Add rpm packages records to metadata.
 * @since 1.10
 */
public final class AstoMetadataAdd {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Repository config.
     */
    private final RepoConfig cnfg;

    /**
     * Ctor.
     * @param asto Asto storage
     * @param cnfg Repository config
     */
    public AstoMetadataAdd(final Storage asto, final RepoConfig cnfg) {
        this.asto = asto;
        this.cnfg = cnfg;
    }

    /**
     * Adds provided packages collection to metadata.
     * @param metas Packages metadata to add
     * @return Completable action with temp key
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    public CompletionStage<Key> perform(final Collection<Package.Meta> metas) {
        final Key prefix = new Key.From(UUID.randomUUID().toString());
        return this.addToPrimary(prefix, metas).thenCompose(
            res -> {
                final CompletableFuture<Void> future;
                if (this.cnfg.filelists()) {
                    future = CompletableFuture.allOf(
                        this.add(prefix, metas, res, XmlPackage.OTHER, new XmlEvent.Other()),
                        this.add(prefix, metas, res, XmlPackage.FILELISTS, new XmlEvent.Filelists())
                    );
                } else {
                    future = this.add(prefix, metas, res, XmlPackage.OTHER, new XmlEvent.Other());
                }
                return future;
            }
        ).thenCompose(
            ignored -> this.asto.list(prefix).thenCompose(
                list -> CompletableFuture.allOf(
                    list.stream().map(
                        item -> new AstoChecksumAndSize(this.asto, this.cnfg.digest())
                            .calculate(item)
                            .thenCompose(nothing -> new AstoArchive(this.asto).gzip(item))
                    ).toArray(CompletableFuture[]::new)
                )
            )
        ).thenApply(nothing -> prefix);
    }

    /**
     * Adds items to primary and returns the result.
     * @param temp Temp location
     * @param metas Packages metadata to add
     * @return Completable action with the result
     */
    private CompletionStage<MergedXml.Result> addToPrimary(
        final Key temp, final Collection<Package.Meta> metas
    ) {
        return this.getExistingOrDefaultKey(XmlPackage.PRIMARY).thenCompose(
            key -> {
                final Key tempkey = new Key.From(temp, XmlPackage.PRIMARY.name());
                return new StorageValuePipeline<MergedXml.Result>(this.asto, key, tempkey)
                    .processWithResult(
                        (input, out) -> new UncheckedScalar<>(
                            () -> new MergedXmlPrimary(
                                input.map(new UncheckedIOFunc<>(GZIPInputStream::new)), out
                            ).merge(metas, new XmlEventPrimary())
                        ).value()
                    ).thenApply(
                        res -> res
                    ).thenCompose(
                        res -> new StorageValuePipeline<>(this.asto, tempkey).process(
                            (input, out) -> new XmlAlter.Stream(
                                new BufferedInputStream(input.get()),
                                new BufferedOutputStream(out)
                            ).pkgAttr(XmlPackage.PRIMARY.tag(), String.valueOf(res.count()))
                        ).thenApply(nothing -> res)
                    ).thenApply(
                        res -> res
                    );
            }
        );
    }

    /**
     * Adds packages metadata to metadata file.
     * @param temp Temp location
     * @param metas Packages metadata to add
     * @param primary Result of adding packages to primary xml
     * @param type Metadata type
     * @param event Xml event instance
     * @return COmpletable action
         */
    private CompletableFuture<Void> add(final Key temp, final Collection<Package.Meta> metas,
        final MergedXml.Result primary, final XmlPackage type, final XmlEvent event) {
        return this.getExistingOrDefaultKey(type).thenCompose(
            key -> {
                final Key tempkey = new Key.From(temp, type.name());
                return new StorageValuePipeline<>(this.asto, key, tempkey).process(
                    (input, out) -> new UncheckedScalar<>(
                        () -> new MergedXmlPackage(
                            input.map(new UncheckedIOFunc<>(GZIPInputStream::new)),
                            out, type, primary
                        ).merge(metas, event)
                    ).value()
                );
            }
        ).toCompletableFuture();
    }

    /**
     * Find existing metadata key or return default key. Item with default key does not actually
     * exist in storage, but later this key is used in {@link StorageValuePipeline}
     * which handle the situation correctly.
     * @param type Metadata type
     * @return Completable action with the key
     */
    private CompletionStage<Key> getExistingOrDefaultKey(final XmlPackage type) {
        final String key = String.format("%s.xml.gz", type.lowercase());
        return this.asto.list(new Key.From("repodata")).thenApply(
            list -> list.stream().filter(item -> item.string().endsWith(key))
                .findFirst().orElse(new Key.From(key))
        );
    }
}
