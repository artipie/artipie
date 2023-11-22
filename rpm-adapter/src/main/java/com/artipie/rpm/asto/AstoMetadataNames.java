/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.meta.XmlPackage;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Prepares new names for metadata files.
 * @since 1.10
 */
final class AstoMetadataNames {

    /**
     * Repomd xml name.
     */
    private static final String REPOMD = "repomd.xml";

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
    AstoMetadataNames(final Storage asto, final RepoConfig cnfg) {
        this.asto = asto;
        this.cnfg = cnfg;
    }

    /**
     * Prepares correct names for metadata from temp location. In the temp location
     * metadata are named by {@link XmlPackage#name()}, repomd.xml is also located in temp.
     * New names are created in correspondence with naming policy {@link RepoConfig#naming()} and
     * in canonical repository metadata location `metadata` directory.
     * This method does not move the metadata items, only constructs names.
     * @param temp Temp location
     * @return Map of the temp metadata location -> location in the repository
     */
    CompletionStage<Map<Key, Key>> prepareNames(final Key temp) {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.asto);
        return rxsto.list(temp)
            .flatMapObservable(Observable::fromIterable)
            .filter(
                key -> new XmlPackage.Stream(this.cnfg.filelists()).get()
                    .anyMatch(item -> key.string().endsWith(item.name()))
            )
            .<Pair<Key, Key>>flatMapSingle(
                key -> rxsto.value(key).flatMap(
                    val -> Single.fromFuture(
                        new ContentDigest(val, () -> this.cnfg.digest().messageDigest()).hex()
                            .thenApply(
                                hex -> new ImmutablePair<Key, Key>(
                                    key,
                                    new Key.From(
                                        this.cnfg.naming().fullName(
                                            new XmlPackage.Stream(this.cnfg.filelists()).get()
                                                .filter(item -> key.string().contains(item.name()))
                                                .findFirst().get(),
                                            hex
                                        )
                                    )
                                )
                            ).toCompletableFuture()
                    )
                )
            ).toMap(Pair::getKey, Pair::getValue)
            .flatMap(
                map -> {
                    final Key.From repomd = new Key.From(temp, AstoMetadataNames.REPOMD);
                    return rxsto.exists(repomd).map(
                        exists -> {
                            if (exists) {
                                map.put(repomd, new Key.From("repodata", AstoMetadataNames.REPOMD));
                            }
                            return map;
                        }
                    );
                }
            ).to(SingleInterop.get());
    }
}
