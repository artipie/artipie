/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021-2023 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package  com.artipie.conan;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.lock.Lock;
import com.artipie.asto.lock.storage.StorageLock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Conan V2 API - main revisions index APIs. Revisions index stored in revisions.txt file
 * in json format.
 * There are 2+ index files: recipe revisions and binary revisions (per package).
 * @since 0.1
 */
public final class RevisionsIndexApi {

    /**
     * Revisions index file name.
     */
    private static final String INDEX_FILE = "revisions.txt";

    /**
     * Package recipe (sources) subdir name.
     */
    private static final String SRC_SUBDIR = "export";

    /**
     * Package binaries subdir name.
     */
    private static final String BIN_SUBDIR = "package";

    /**
     * RevisionsIndex core logic.
     */
    private final RevisionsIndexCore core;

    /**
     * Revision info indexer.
     */
    private final RevisionsIndexer indexer;

    /**
     * Revision info indexer.
     */
    private final FullIndexer fullindexer;

    /**
     * Current Artipie storage instance.
     */
    private final Storage storage;

    /**
     * Package key for repository data.
     */
    private final Key pkgkey;

    /**
     * Initializes new instance.
     * @param storage Current Artipie storage instance.
     * @param pkgkey Package key for repository package data (full name).
     */
    public RevisionsIndexApi(final Storage storage, final Key pkgkey) {
        this.storage = storage;
        this.pkgkey = pkgkey;
        this.core = new RevisionsIndexCore(storage);
        this.indexer = new RevisionsIndexer(storage);
        this.fullindexer = new FullIndexer(storage, this.indexer);
    }

    /**
     * Updates recipe index file, non recursive, doesn't affect package binaries.
     * @return CompletableFuture with recipe revisions list.
     */
    public CompletionStage<List<Integer>> updateRecipeIndex() {
        return this.doWithLock(
            this.pkgkey, () -> this.indexer.buildIndex(
                this.pkgkey, PackageList.PKG_SRC_LIST, (name, rev) -> new Key.From(
                    this.pkgkey.string(), rev.toString(), RevisionsIndexApi.SRC_SUBDIR, name
                )
            ));
    }

    /**
     * Updates binary index file.
     * @param reciperev Recipe revision number.
     * @param hash Target package binary hash.
     * @return CompletableFuture with recipe revisions list.
     */
    public CompletionStage<List<Integer>> updateBinaryIndex(final int reciperev,
        final String hash) {
        final Key key = new Key.From(
            this.pkgkey.string(), Integer.toString(reciperev),
            RevisionsIndexApi.BIN_SUBDIR, hash
        );
        return this.doWithLock(
            this.pkgkey, () -> this.indexer.buildIndex(
                key, PackageList.PKG_BIN_LIST, (name, rev) -> new Key.From(
                    key.string(), rev.toString(), name
                )
            ));
    }

    /**
     * Updates binary index file. Fully recursive.
     * Does updateRecipeIndex(), then for each revision & for each pkg binary updateBinaryIndex().
     * @return CompletionStage to handle operation completion.
     */
    public CompletionStage<Void> fullIndexUpdate() {
        return this.doWithLock(
            new Key.From(this.pkgkey), () -> this.fullindexer.fullIndexUpdate(this.pkgkey)
        );
    }

    /**
     * Add new revision to the recipe index.
     * @param revision Revision number.
     * @return CompletionStage for this operation.
     */
    public CompletionStage<Void> addRecipeRevision(final int revision) {
        final Key key = this.getRecipeRevkey();
        return this.doWithLock(
            new Key.From(key), () -> this.core.addToRevdata(revision, key)
        );
    }

    /**
     * Returns list of revisions for the recipe.
     * @return CompletionStage with the list.
     */
    public CompletionStage<List<Integer>> getRecipeRevisions() {
        return this.core.getRevisions(this.getRecipeRevkey());
    }

    /**
     * Removes specified revision from index file of package recipe.
     * @param revision Revision number.
     * @return CompletionStage with boolean == true if recipe & revision were found.
     */
    public CompletionStage<Boolean> removeRecipeRevision(final int revision) {
        final Key key = this.getRecipeRevkey();
        return this.doWithLock(
            new Key.From(key), () -> this.core.removeRevision(revision, key)
        );
    }

    /**
     * Returns last (max) recipe revision value.
     * @return CompletableFuture with recipe revision as Integer.
     */
    public CompletableFuture<Integer> getLastRecipeRevision() {
        return this.core.getLastRev(this.getRecipeRevkey());
    }

    /**
     * Returns list of revisions for the package binary.
     * @param reciperev Recipe revision number.
     * @param hash Target package binary hash.
     * @return CompletionStage with the list.
     */
    public CompletionStage<List<Integer>> getBinaryRevisions(final int reciperev,
        final String hash) {
        return this.core.getRevisions(this.getBinaryRevkey(reciperev, hash));
    }

    /**
     * Returns last (max) revision number for binary revision index.
     * @param reciperev Recipe revision number.
     * @param hash Target package binary hash.
     * @return CompletableFuture with recipe revision as Integer.
     */
    public CompletableFuture<Integer> getLastBinaryRevision(final int reciperev,
        final String hash) {
        return this.core.getLastRev(this.getBinaryRevkey(reciperev, hash));
    }

    /**
     * Removes specified revision from index file of package binary.
     * @param reciperev Recipe revision number.
     * @param hash Target package binary hash.
     * @param revision Revision number of the binary.
     * @return CompletionStage with boolean == true if recipe & revision were found.
     */
    public CompletionStage<Boolean> removeBinaryRevision(final int reciperev, final String hash,
        final int revision) {
        final Key key = this.getBinaryRevkey(reciperev, hash);
        return this.doWithLock(
            new Key.From(key), () -> this.core.removeRevision(revision, key)
        );
    }

    /**
     * Add binary revision to the index.
     * @param reciperev Recipe revision number.
     * @param hash Package binary hash.
     * @param revision Package binary revision.
     * @return CompletionStage to handle operation completion.
     */
    public CompletionStage<Void> addBinaryRevision(final int reciperev, final String hash,
        final int revision) {
        final Key key = this.getBinaryRevkey(reciperev, hash);
        return this.doWithLock(
            new Key.From(key), () -> this.core.addToRevdata(revision, key)
        );
    }

    /**
     * Returns binary packages list (of hashes) for given recipe revision.
     * @param reciperev Revision number of the recipe.
     * @return CompletionStage with the list of package binaries (hashes) as strings.
     */
    public CompletionStage<List<String>> getPackageList(final int reciperev) {
        final Key key = new Key.From(
            this.pkgkey, Integer.toString(reciperev), RevisionsIndexApi.BIN_SUBDIR
        );
        return new PackageList(this.storage).get(key);
    }

    /**
     * Creates full storage key to the binary revisions file.
     * @param reciperev Recipe revision number.
     * @param hash Target package binary hash.
     * @return Binary revisions index file in the storage, as Key.
     */
    private Key getBinaryRevkey(final int reciperev, final String hash) {
        return new Key.From(
            this.pkgkey, Integer.toString(reciperev), RevisionsIndexApi.BIN_SUBDIR,
            hash, RevisionsIndexApi.INDEX_FILE
        );
    }

    /**
     * Creates full storage key to the recipe revisions file.
     * @return Recipe revisions index file in the storage, as Key.
     */
    private Key getRecipeRevkey() {
        return new Key.From(this.pkgkey, RevisionsIndexApi.INDEX_FILE);
    }

    /**
     * Performs operation under lock on target with one hour expiration time.
     * @param target Lock target key.
     * @param operation Operation.
     * @param <T> Return type for operation's CompletableFuture.
     * @return Completion of operation and lock.
     */
    private <T> CompletionStage<T> doWithLock(final Key target,
        final Supplier<CompletionStage<T>> operation) {
        final Lock lock = new StorageLock(
            this.storage, target, Instant.now().plus(Duration.ofHours(1))
        );
        return lock.acquire().thenCompose(
            nothing -> operation.get().thenApply(
                result -> {
                    lock.release();
                    return result;
                }));
    }
}
