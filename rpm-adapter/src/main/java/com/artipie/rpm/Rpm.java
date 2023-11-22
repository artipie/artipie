/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.lock.Lock;
import com.artipie.asto.lock.storage.StorageLock;
import com.artipie.asto.misc.UncheckedIOScalar;
import com.artipie.asto.streams.ContentAsStream;
import com.artipie.rpm.asto.AstoChecksumAndName;
import com.artipie.rpm.asto.AstoRepoAdd;
import com.artipie.rpm.asto.AstoRepoRemove;
import com.artipie.rpm.http.RpmUpload;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlPrimaryChecksums;
import com.artipie.rpm.misc.PackagesDiff;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * The RPM front.
 *
 * First, you make an instance of this class, providing
 * your storage as an argument:
 *
 * <pre> Rpm rpm = new Rpm(storage);</pre>
 *
 * Then, you put your binary RPM artifact to the storage and call
 * {@link Rpm#batchUpdate(Key)}. This method will parse the all RPM packages
 * in repository and update all the necessary meta-data files. Right after this,
 * your clients will be able to use the package, via {@code yum}:
 *
 * <pre> rpm.batchUpdate(new Key.From("rmp-repo"));</pre>
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ClassFanOutComplexityCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class Rpm {

    /**
     * Primary storage.
     */
    private final Storage storage;

    /**
     * Repository configuration.
     */
    private final RepoConfig config;

    /**
     * New Rpm for repository in storage. Does not include filelists.xml in update.
     * @param stg The storage which contains repository
     */
    public Rpm(final Storage stg) {
        this(stg, StandardNamingPolicy.PLAIN, Digest.SHA256, false);
    }

    /**
     * New Rpm for repository in storage.
     * @param stg The storage which contains repository
     * @param filelists Include file lists in update
     */
    public Rpm(final Storage stg, final boolean filelists) {
        this(stg, StandardNamingPolicy.PLAIN, Digest.SHA256, filelists);
    }

    /**
     * Ctor.
     * @param stg The storage
     * @param naming RPM files naming policy
     * @param dgst Hashing sum computation algorithm
     * @param filelists Include file lists in update
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    public Rpm(final Storage stg, final NamingPolicy naming, final Digest dgst,
        final boolean filelists) {
        this(stg, new RepoConfig.Simple(dgst, naming, filelists));
    }

    /**
     * Ctor.
     * @param storage The storage
     * @param config Repository configuration
     */
    public Rpm(final Storage storage, final RepoConfig config) {
        this.storage = storage;
        this.config = config;
    }

    /**
     * Update the meta info for single artifact.
     *
     * @param key The name of the file just updated
     * @return Completion or error signal.
     * @deprecated use {@link #update(Key)} instead
     */
    @Deprecated
    public Completable update(final String key) {
        return this.update(new Key.From(key));
    }

    /**
     * Update the meta info for single artifact.
     *
     * @param key The name of the file just updated
     * @return Completion or error signal.
     * @deprecated This method calls {@link #batchUpdate(Key)} with parent of the key
     */
    @Deprecated
    public Completable update(final Key key) {
        final String[] parts = key.string().split("/");
        final Key folder;
        if (parts.length == 1) {
            folder = Key.ROOT;
        } else {
            folder = new Key.From(
                Arrays.stream(parts)
                    .limit(parts.length - 1)
                    .toArray(String[]::new)
            );
        }
        return this.batchUpdate(folder);
    }

    /**
     * Batch update RPM files for repository.
     * @param prefix Repository key prefix (String)
     * @return Completable action
     * @deprecated use {@link #batchUpdate(Key)} instead
     */
    @Deprecated
    public Completable batchUpdate(final String prefix) {
        return this.batchUpdate(new Key.From(prefix));
    }

    /**
     * Batch update RPM files for repository.
     * @param prefix Repository key prefix
     * @return Completable action
     * @throws ArtipieIOException On IO-operation errors
     */
    public Completable batchUpdate(final Key prefix) {
        return this.doWithLock(
            prefix,
            () -> Completable.fromFuture(this.calcDiff(prefix).thenCompose(
                list -> {
                    final Storage sub = new SubStorage(prefix, this.storage);
                    return new AstoRepoAdd(sub, this.config).perform().thenCompose(
                        nothing -> new AstoRepoRemove(sub, this.config).perform(list)
                    );
                }).toCompletableFuture()
            )
        );
    }

    /**
     * Batch update RPM files for repository,
     * works exactly as {@link Rpm#batchUpdate(Key)}.
     * @param prefix Repo prefix
     * @return Completable action
     * @throws ArtipieIOException On IO-operation errors
     * @deprecated Use {@link Rpm#batchUpdate(Key)}
     */
    @Deprecated
    public Completable batchUpdateIncrementally(final Key prefix) {
        return this.batchUpdate(prefix);
    }

    /**
     * Performs operation under root lock with one hour expiration time.
     *
     * @param target Lock target key.
     * @param operation Operation.
     * @return Completion of operation and lock.
     */
    private Completable doWithLock(final Key target, final Supplier<Completable> operation) {
        final Lock lock = new StorageLock(
            this.storage,
            target,
            Instant.now().plus(Duration.ofHours(1))
        );
        return Completable.fromFuture(
            lock.acquire()
                .thenCompose(nothing -> operation.get().to(CompletableInterop.await()))
                .thenCompose(nothing -> lock.release())
                .toCompletableFuture()
        );
    }

    /**
     * Calculate differences between current metadata and storage rpms, prepare
     * packages to add or to remove.
     * @param prefix Prefix key
     * @return Completable action with list of the checksums of the remove packages
     */
    private CompletionStage<Collection<String>> calcDiff(final Key prefix) {
        return this.storage.list(new Key.From(prefix, "repodata"))
            .thenApply(
                list -> list.stream().filter(
                    item -> item.string().contains(XmlPackage.PRIMARY.lowercase())
                        && item.string().endsWith("xml.gz")
                ).findFirst()
            ).thenCompose(
                opt -> {
                    final CompletionStage<Collection<String>> res;
                    final SubStorage sub = new SubStorage(prefix, this.storage);
                    if (opt.isPresent()) {
                        res = this.storage.value(opt.get()).thenCompose(
                            val -> new ContentAsStream<Map<String, String>>(val).process(
                                input -> new XmlPrimaryChecksums(
                                    new UncheckedIOScalar<>(() -> new GZIPInputStream(input))
                                        .value()
                                ).read()
                            )
                        ).thenCompose(
                            primary -> new AstoChecksumAndName(this.storage, this.config.digest())
                                .calculate(prefix)
                                .thenApply(repo -> new PackagesDiff(primary, repo))
                        ).thenCompose(
                            diff -> Rpm.copyPackagesToAdd(
                                sub,
                                diff.toAdd().stream().map(Key.From::new)
                                    .collect(Collectors.toList())
                            ).thenApply(nothing -> diff.toDelete().values())
                        );
                    } else {
                        res = sub.list(Key.ROOT).thenApply(
                            list -> list.stream().filter(item -> item.string().endsWith("rpm"))
                        ).thenCompose(
                            rpms -> copyPackagesToAdd(sub, rpms.collect(Collectors.toList()))
                        ).thenApply(nothing -> Collections.emptySet());
                    }
                    return res;
                }
            );
    }

    /**
     * Handles packages that should be added to metadata.
     * @param asto Storage
     * @param rpms Packages
     * @return Completable action
     */
    private static CompletableFuture<Void> copyPackagesToAdd(
        final Storage asto, final List<Key> rpms
    ) {
        return new Copy(asto, rpms).copy(new SubStorage(RpmUpload.TO_ADD, asto));
    }
}
