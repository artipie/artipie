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
import hu.akarnokd.rxjava2.interop.FlowableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Conan V2 API revisions index (re)generation support.
 * Revisions index stored in revisions.txt file in json format.
 * There are 2+ index files: recipe revisions and binary revisions (per package).
 * @since 0.1
 */
public class FullIndexer {

    /**
     * Package recipe (sources) subdir name.
     */
    private static final String SRC_SUBDIR = "export";

    /**
     * Package binaries subdir name.
     */
    private static final String BIN_SUBDIR = "package";

    /**
     * Current Artipie storage instance.
     */
    private final Storage storage;

    /**
     * Revision info indexer.
     */
    private final RevisionsIndexer indexer;

    /**
     * Initializes instance of indexer.
     * @param storage Current Artipie storage instance.
     * @param indexer Revision info indexer.
     */
    public FullIndexer(final Storage storage, final RevisionsIndexer indexer) {
        this.storage = storage;
        this.indexer = indexer;
    }

    /**
     * Updates binary index file. Fully recursive.
     * Does updateRecipeIndex(), then for each revision & for each pkg binary updateBinaryIndex().
     * @param key Key in the Artipie Storage for the revisions index file.
     * @return CompletionStage to handle operation completion.
     */
    public CompletionStage<Void> fullIndexUpdate(final Key key) {
        final Flowable<List<Integer>> flowable = SingleInterop.fromFuture(
            this.indexer.buildIndex(
                key, PackageList.PKG_SRC_LIST, (name, rev) -> new Key.From(
                    key, rev.toString(), FullIndexer.SRC_SUBDIR, name
                )
            )).flatMapPublisher(Flowable::fromIterable).flatMap(
                rev -> {
                    final Key packages = new Key.From(
                        key, rev.toString(), FullIndexer.BIN_SUBDIR
                    );
                    return SingleInterop.fromFuture(
                        new PackageList(this.storage).get(packages).thenApply(
                            pkgs -> pkgs.stream().map(
                                pkg -> new Key.From(packages, pkg)
                            ).collect(Collectors.toList())
                        )
                    ).flatMapPublisher(Flowable::fromIterable);
                })
            .flatMap(
                pkgkey -> FlowableInterop.fromFuture(
                    this.indexer.buildIndex(
                        pkgkey, PackageList.PKG_BIN_LIST, (name, rev) ->
                            new Key.From(pkgkey, rev.toString(), name)
                    )
                )
            )
            .parallel().runOn(Schedulers.io())
            .sequential().observeOn(Schedulers.io());
        return flowable.toList().to(SingleInterop.get()).thenCompose(
            unused -> CompletableFuture.completedFuture(null)
        );
    }
}
