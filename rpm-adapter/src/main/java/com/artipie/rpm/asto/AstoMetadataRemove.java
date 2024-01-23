/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.asto.misc.UncheckedIOScalar;
import com.artipie.asto.streams.StorageValuePipeline;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.meta.PackageInfo;
import com.artipie.rpm.meta.XmlAlter;
import com.artipie.rpm.meta.XmlMaid;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlPrimaryMaid;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Removes packages from metadata files.
 * @since 1.9
 */
public final class AstoMetadataRemove {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Repository config.
     */
    private final RepoConfig cnfg;

    /**
     * Collection with removed packages info if required.
     */
    private final Optional<Collection<PackageInfo>> infos;

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param cnfg Repos config
     * @param infos Collection with removed packages info if required
     */
    public AstoMetadataRemove(final Storage asto, final RepoConfig cnfg,
        final Optional<Collection<PackageInfo>> infos) {
        this.asto = asto;
        this.cnfg = cnfg;
        this.infos = infos;
    }

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param cnfg Repos config
     */
    public AstoMetadataRemove(final Storage asto, final RepoConfig cnfg) {
        this(asto, cnfg, Optional.empty());
    }

    /**
     * Removes packages from metadata xmls. Resulting new xmls are stored into temp location
     * along with checksums and size of un-gziped files. Temp location key as returned in result.
     * @param checksums Checksums of the packages to remove
     * @return Completable action with temp location key
     */
    @SuppressWarnings("rawtypes")
    public CompletionStage<Key> perform(final Collection<String> checksums) {
        final List<CompletableFuture<Void>> res = new ArrayList<>(3);
        final Key.From prefix = new Key.From(UUID.randomUUID().toString());
        for (final XmlPackage pckg : new XmlPackage.Stream(this.cnfg.filelists())
            .get().collect(Collectors.toList())) {
            res.add(
                CompletableFuture.supplyAsync(() -> pckg).thenCompose(
                    pkg -> this.asto.list(new Key.From("repodata")).thenApply(
                        list -> list.stream()
                            .filter(item -> item.string().contains(pckg.lowercase())).findFirst()
                    ).thenCompose(
                        opt -> {
                            final Key tmpkey = new Key.From(prefix, pkg.name());
                            CompletionStage<Void> result = CompletableFuture.allOf();
                            if (opt.isPresent()) {
                                result = this.removePackages(pckg, opt.get(), tmpkey, checksums)
                                    .thenCompose(
                                        cnt -> new StorageValuePipeline<>(this.asto, tmpkey)
                                            .process(
                                                (inpt, out) -> new XmlAlter.Stream(
                                                    new BufferedInputStream(inpt.get()),
                                                    new BufferedOutputStream(out)
                                                ).pkgAttr(pckg.tag(), String.valueOf(cnt))
                                        )
                                    ).thenCompose(
                                        nothing -> new AstoChecksumAndSize(
                                            this.asto, this.cnfg.digest()
                                        ).calculate(tmpkey)
                                    )
                                    .thenCompose(hex -> new AstoArchive(this.asto).gzip(tmpkey));
                            }
                            return result;
                        }
                    )
                )
            );
        }
        return CompletableFuture.allOf(res.toArray(new CompletableFuture[]{}))
            .thenApply(nothing -> prefix);
    }

    /**
     * Removes packages from metadata file.
     * @param pckg Package type
     * @param key Item key
     * @param temp Temp key where to write the result
     * @param checksums Checksums to remove
     * @return Completable action with count of the items left in storage
         */
    private CompletionStage<Long> removePackages(
        final XmlPackage pckg, final Key key, final Key temp, final Collection<String> checksums
    ) {
        return new StorageValuePipeline<Long>(this.asto, key, temp).processWithResult(
            (opt, out) -> {
                final XmlMaid maid;
                final InputStream input = opt.map(new UncheckedIOFunc<>(GZIPInputStream::new))
                    .get();
                if (pckg == XmlPackage.PRIMARY) {
                    maid = new XmlPrimaryMaid.Stream(input, out, this.infos);
                } else {
                    maid = new XmlMaid.ByPkgidAttr.Stream(input, out);
                }
                return new UncheckedIOScalar<>(() -> maid.clean(checksums)).value();
            }
        );
    }

}
