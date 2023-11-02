/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentAs;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.asto.streams.StorageValuePipeline;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlRepomd;
import com.artipie.rpm.pkg.Checksum;
import com.jcabi.aspects.Tv;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Creates `repomd.xml`.
 * @since 1.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AstoCreateRepomd {

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
    public AstoCreateRepomd(final Storage asto, final RepoConfig cnfg) {
        this.asto = asto;
        this.cnfg = cnfg;
    }

    /**
     * Creates repomd.xml.
     * @param temp Temp location to read/write data
     * @return Completable action
     */
    public CompletionStage<Void> perform(final Key temp) {
        return this.openChecksums(temp).thenCompose(
            open -> this.gzipedChecksums(temp).thenCompose(
                gziped -> new StorageValuePipeline<>(this.asto, new Key.From(temp, "repomd.xml"))
                    .process(
                        (opt, out) -> {
                            try (XmlRepomd repomd = new XmlRepomd(out)) {
                                repomd.begin(System.currentTimeMillis() / Tv.THOUSAND);
                                new XmlPackage.Stream(this.cnfg.filelists()).get()
                                    .filter(
                                        item -> gziped.containsKey(item) && open.containsKey(item)
                                    ).forEach(
                                        type -> {
                                            try (XmlRepomd.Data data =
                                                repomd.beginData(type.lowercase())) {
                                                final Checksum gzsum = this.checksum(gziped, type);
                                                data.gzipChecksum(gzsum);
                                                data.openChecksum(this.checksum(open, type));
                                                data.location(
                                                    this.cnfg.naming().fullName(type, gzsum.hex())
                                                );
                                                data.gzipSize(AstoCreateRepomd.size(gziped, type));
                                                data.openSize(AstoCreateRepomd.size(open, type));
                                            } catch (final XMLStreamException | IOException err) {
                                                throw new ArtipieIOException(
                                                    "Failed to update repomd.xml", err
                                                );
                                            }
                                        }
                                    );
                            }
                        }
                    )
            )
        );
    }

    /**
     * Reads gziped checksums from temp locations.
     * @param temp Temp location
     * @return Map of the temp location key and value
     */
    private CompletionStage<Map<XmlPackage, String>> gzipedChecksums(final Key temp) {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.asto);
        return rxsto.list(temp)
            .flatMapObservable(Observable::fromIterable)
            .filter(key -> !key.string().endsWith(this.cnfg.digest().name()))
            .<Pair<XmlPackage, String>>flatMapSingle(
                key -> rxsto.value(key).flatMap(
                    val -> Single.fromFuture(
                        new ContentDigest(
                            val,
                            () -> this.cnfg.digest().messageDigest()
                        ).hex().thenApply(
                            hex -> new ImmutablePair<>(
                                this.pckgType(key), String.format("%s %d", hex, val.size().get())
                            )
                        ).toCompletableFuture()
                    )
                )
            )
            .toMap(Pair::getKey, Pair::getValue)
            .to(SingleInterop.get());
    }

    /**
     * Reads open checksums from temp locations.
     * @param temp Temp location
     * @return Map of the metadata package type and value
     */
    private CompletionStage<Map<XmlPackage, String>> openChecksums(final Key temp) {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.asto);
        return rxsto.list(temp)
            .flatMapObservable(Observable::fromIterable)
            .filter(key -> key.string().endsWith(this.cnfg.digest().name()))
            .<Pair<XmlPackage, String>>flatMapSingle(
                key -> rxsto.value(key).to(ContentAs.STRING)
                    .map(str -> new ImmutablePair<>(this.pckgType(key), str))
            )
            .toMap(Pair::getKey, Pair::getValue)
            .to(SingleInterop.get());
    }

    /**
     * Obtain instance of {@link XmlPackage} by temp key name.
     * @param key Temp key
     * @return Instance of {@link XmlPackage}
     * @throws ArtipieException If not recognized
     */
    private XmlPackage pckgType(final Key key) {
        return new XmlPackage.Stream(this.cnfg.filelists()).get()
            .filter(item -> key.string().contains(item.name())).findFirst()
            .orElseThrow(() -> new ArtipieException("Unknown metadata file name!"));
    }

    /**
     * Create instance of {@link Checksum}.
     * @param map Map with checksum data
     * @param type Package metadata type
     * @return Checksum
     */
    private Checksum checksum(
        final Map<XmlPackage, String> map, final XmlPackage type
    ) {
        return new Checksum.Simple(this.cnfg.digest(), map.get(type).split(" ")[0]);
    }

    /**
     * Obtain size from map by package type.
     * @param map Map to get size from
     * @param type Package metadata type
     * @return Package size
     */
    private static long size(final Map<XmlPackage, String> map, final XmlPackage type) {
        return Long.parseLong(map.get(type).split(" ")[1]);
    }
}
