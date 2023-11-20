/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import io.vavr.Tuple2;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArrayBuilder;

/**
 * Conan V2 API revisions index (re)generation support.
 * Revisions index stored in revisions.txt file in json format.
 * There are 2+ index files: recipe revisions and binary revisions (per package).
 * @since 0.1
 */
public class RevisionsIndexer {

    /**
     * Revisions index file name.
     */
    private static final String INDEX_FILE = "revisions.txt";

    /**
     * Current Artipie storage instance.
     */
    private final Storage storage;

    /**
     * Initializes new instance.
     * @param storage Current Artipie storage instance.
     */
    public RevisionsIndexer(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Rebuilds specified revision index (WIP). Extracts revisions lists, check files presense,
     * then creates revision index files with valid revision numbers.
     * @param key Index file directory key (package path).
     * @param pkgfiles Package files list for verification.
     * @param generator Generates full key value to one of the pkgfiles. (name, rev) -> key.
     * @return CompletableFuture with recipe revisions list.
     */
    @SuppressWarnings("PMD.UseVarargs")
    public CompletionStage<List<Integer>> buildIndex(final Key key,
        final List<String> pkgfiles, final BiFunction<String, Integer, Key> generator) {
        final CompletionStage<List<Integer>> revisions = new PackageList(this.storage).get(
            key
        ).thenCompose(
            list -> {
                final List<Tuple2<Integer, CompletableFuture<Boolean>>> revchecks =
                    list.stream().map(
                        rev -> {
                            final Integer revnum = Integer.parseInt(rev);
                            return new Tuple2<>(
                                revnum, this.checkPkgRevValid(revnum, pkgfiles, generator)
                            );
                        }).collect(Collectors.toList());
                return new Completables.JoinTuples<>(revchecks).toTuples().thenApply(
                    checks -> checks.stream().filter(Tuple2::_2).map(Tuple2::_1)
                        .collect(Collectors.toList())
                );
            });
        return revisions.thenCompose(
            revs -> {
                final JsonArrayBuilder builder = Json.createArrayBuilder();
                revs.stream().map(rev -> new PkgRev(rev).toJson()).forEach(builder::add);
                final Key revkey = new Key.From(key.string(), RevisionsIndexer.INDEX_FILE);
                return this.storage.save(
                    new Key.From(revkey), new RevContent(builder.build()).toContent()
                ).thenApply(nothing -> revs);
            });
    }

    /**
     * Checks that package revision contents is valid.
     * @param rev Revision number in the package.
     * @param pkgfiles Package files list for verification.
     * @param generator Generates full key value to one of the pkgfiles. (name, rev) -> key.
     * @return CompletableFuture with package validity result.
     */
    private CompletableFuture<Boolean> checkPkgRevValid(final Integer rev,
        final List<String> pkgfiles, final BiFunction<String, Integer, Key> generator) {
        final List<CompletableFuture<Boolean>> checks = pkgfiles.stream().map(
            name -> this.storage.exists(
                new Key.From(generator.apply(name, rev))
            )).collect(Collectors.toList());
        return new Completables.JoinList<>(checks).toList().thenApply(
            results -> results.stream().allMatch(v -> v)
        );
    }
}
