/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.key.KeyExcludeFirst;
import com.artipie.asto.lock.storage.StorageLock;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.http.RpmRemove;
import com.artipie.rpm.meta.PackageInfo;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Workflow to remove packages from repository.
 * @since 1.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AstoRepoRemove {

    /**
     * Metadata key.
     */
    private static final Key META = new Key.From("repodata");

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
     * @param cnfg Repository config
     * @param infos Collection with removed packages info if required
     */
    public AstoRepoRemove(final Storage asto, final RepoConfig cnfg,
        final Optional<Collection<PackageInfo>> infos) {
        this.asto = asto;
        this.cnfg = cnfg;
        this.infos = infos;
    }

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param cnfg Repository config
     * @param infos Collection with removed packages info
     */
    public AstoRepoRemove(final Storage asto, final RepoConfig cnfg,
        final Collection<PackageInfo> infos) {
        this(asto, cnfg, Optional.of(infos));
    }

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param cnfg Repository config
     */
    public AstoRepoRemove(final Storage asto, final RepoConfig cnfg) {
        this(asto, cnfg, Optional.empty());
    }

    /**
     * Performs whole workflow to remove items by provided checksums from
     * the repository. Rpm packages themselves are considered to be already removed
     * from the repository.
     * @param checksums Checksums of the packages to remove to
     * @return Completable action
     */
    public CompletionStage<Void> perform(final Collection<String> checksums) {
        return new AstoMetadataRemove(this.asto, this.cnfg, this.infos).perform(checksums)
            .thenCompose(
                temp -> new AstoCreateRepomd(this.asto, this.cnfg).perform(temp).thenCompose(
                    nothing -> new AstoMetadataNames(this.asto, this.cnfg).prepareNames(temp)
                        .thenCompose(
                            keys -> {
                                final StorageLock lock =
                                    new StorageLock(this.asto, AstoRepoRemove.META);
                                return lock.acquire()
                                    .thenCompose(ignored -> this.remove(AstoRepoRemove.META))
                                    .thenCompose(
                                        ignored -> CompletableFuture.allOf(
                                            keys.entrySet().stream().map(
                                                entry ->
                                                    this.asto.move(entry.getKey(), entry.getValue())
                                            ).toArray(CompletableFuture[]::new)
                                        )
                                    ).thenCompose(ignored -> lock.release()).thenCompose(
                                        ignored -> this.remove(temp)
                                    );
                            }
                        )
                )
            );
    }

    /**
     * Performs whole workflow to remove items, listed in {@link RpmRemove#TO_RM} location, from
     * the repository.
     * @return Completable action
     */
    public CompletionStage<Void> perform() {
        return this.checksums().thenCompose(this::perform).thenCompose(
            ignored -> this.asto.list(RpmRemove.TO_RM).thenCompose(
                list -> CompletableFuture.allOf(
                    list.stream().map(
                        key -> this.asto.delete(key).thenCompose(
                            nothing -> this.asto.delete(AstoRepoRemove.removeTemp(key))
                        )
                    ).toArray(CompletableFuture[]::new)
                )
            )
        );
    }

    /**
     * Removes all items found by the key.
     * @param key Key to remove items
     * @return Completable action
     */
    private CompletableFuture<Void> remove(final Key key) {
        return this.asto.list(key).thenCompose(
            list -> CompletableFuture.allOf(
                list.stream().map(this.asto::delete)
                    .toArray(CompletableFuture[]::new)
            )
        );
    }

    /**
     * Calculate checksums of the packages to remove and removes items from
     * temp location {@link RpmRemove#TO_RM}.
     * @return Checksums list
     */
    private CompletionStage<List<String>> checksums() {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.asto);
        return rxsto.list(RpmRemove.TO_RM)
            .flatMapObservable(Observable::fromIterable)
            .map(AstoRepoRemove::removeTemp)
            .flatMapSingle(
                key -> rxsto.value(key).flatMap(
                    val -> Single.fromFuture(
                        new ContentDigest(val, () -> this.cnfg.digest().messageDigest())
                            .hex().toCompletableFuture()
                    )
                )
            ).toList().to(SingleInterop.get());
    }

    /**
     * Removes first {@link RpmRemove#TO_RM} part from the key.
     * @param key Origin key
     * @return Key without {@link RpmRemove#TO_RM} part
     */
    private static Key removeTemp(final Key key) {
        return new KeyExcludeFirst(key, RpmRemove.TO_RM.string());
    }

}
