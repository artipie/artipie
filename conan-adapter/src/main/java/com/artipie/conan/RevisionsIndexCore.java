/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conan;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;

/**
 * Conan V2 API basic revisions index update methods.
 * Revisions index stored in revisions.txt file in json format.
 * There are 2+ index files: recipe revisions and binary revisions (per package).
 * @since 0.1
 */
public class RevisionsIndexCore {

    /**
     * Revisions json field.
     */
    private static final String REVISIONS = "revisions";

    /**
     * Revision json field.
     */
    private static final String REVISION = "revision";

    /**
     * Current Artipie storage instance.
     */
    private final Storage storage;

    /**
     * Initializes new instance.
     * @param storage Current Artipie storage instance.
     */
    public RevisionsIndexCore(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Loads revisions data array from index file.
     * @param key Key for the revisions index.
     * @return CompletableFuture with revisions data as JsonArray.
     */
    public CompletableFuture<JsonArray> loadRevisionData(final Key key) {
        return this.storage.exists(key).thenCompose(
            exist -> {
                final CompletableFuture<JsonArray> revs;
                if (exist) {
                    revs = this.storage.value(key).thenCompose(
                        content -> new PublisherAs(content).asciiString().thenApply(
                            string -> Json.createReader(new StringReader(string)).readObject()
                                .getJsonArray(RevisionsIndexCore.REVISIONS)));
                } else {
                    revs = CompletableFuture.completedFuture(Json.createArrayBuilder().build());
                }
                return revs;
            }
        );
    }

    /**
     * Returns last (max) index file revision value.
     * @param key Key for the revisions index.
     * @return CompletableFuture with index file revision as Integer, or -1 if there's none.
     */
    public CompletableFuture<Integer> getLastRev(final Key key) {
        return this.loadRevisionData(key).thenApply(
            array -> {
                final Optional<JsonValue> max = array.stream().max(
                    (val1, val2) -> {
                        final String revx = val1.asJsonObject()
                            .getString(RevisionsIndexCore.REVISION);
                        final String revy = val2.asJsonObject()
                            .getString(RevisionsIndexCore.REVISION);
                        return Integer.parseInt(revx) - Integer.parseInt(revy);
                    });
                return max.map(
                    jsonValue -> Integer.parseInt(
                        RevisionsIndexCore.getJsonStr(jsonValue, RevisionsIndexCore.REVISION)
                    )).orElse(-1);
            });
    }

    /**
     * Add new revision to the specified index file.
     * @param revision New revision number.
     * @param key Key for the revisions index file.
     * @return CompletionStage for this operation.
     */
    public CompletableFuture<Void> addToRevdata(final int revision, final Key key) {
        return this.loadRevisionData(key).thenCompose(
            array -> {
                final int index = RevisionsIndexCore.jsonIndexOf(
                    array, RevisionsIndexCore.REVISION, revision
                );
                final JsonArrayBuilder updated = Json.createArrayBuilder(array);
                if (index >= 0) {
                    updated.remove(index);
                }
                updated.add(new PkgRev(revision).toJson());
                return this.storage.save(key, new RevContent(updated.build()).toContent());
            });
    }

    /**
     * Removes specified revision from index file.
     * @param revision Revision number.
     * @param key Key for the index file.
     * @return CompletionStage with boolean == true if recipe & revision were found.
     */
    public CompletableFuture<Boolean> removeRevision(final int revision, final Key key) {
        return this.storage.exists(key).thenCompose(
            exist -> {
                final CompletableFuture<Boolean> revs;
                if (exist) {
                    revs = this.storage.value(key).thenCompose(
                        content -> new PublisherAs(content).asciiString().thenCompose(
                            string -> this.removeRevData(string, revision, key)
                        )
                    );
                } else {
                    revs = CompletableFuture.completedFuture(false);
                }
                return revs;
            });
    }

    /**
     * Returns list of revisions for the recipe.
     * @param key Key to the revisions index file.
     * @return CompletionStage with the list.
     */
    public CompletionStage<List<Integer>> getRevisions(final Key key) {
        return this.loadRevisionData(key)
            .thenApply(
                array -> array.stream().map(
                    value -> Integer.parseInt(
                        RevisionsIndexCore.getJsonStr(value, RevisionsIndexCore.REVISION)
                    )).collect(Collectors.toList()));
    }

    /**
     * Extracts string from json object field.
     * @param object Json object.
     * @param key Object key to extract.
     * @return Json object field value as String.
     */
    private static String getJsonStr(final JsonValue object, final String key) {
        return object.asJsonObject().get(key).toString().replaceAll("\"", "");
    }

    /**
     * Removes specified revision from index data.
     * @param content Index file data, as json string.
     * @param revision Revision number.
     * @param target Target file name for save.
     * @return CompletionStage with boolean == true if revision was found.
     */
    private CompletableFuture<Boolean> removeRevData(final String content, final int revision,
        final Key target) {
        final CompletableFuture<Boolean> result;
        final JsonArray revisions = Json.createReader(new StringReader(content)).readObject()
            .getJsonArray(RevisionsIndexCore.REVISIONS);
        final int index = RevisionsIndexCore.jsonIndexOf(
            revisions, RevisionsIndexCore.REVISION, revision
        );
        final JsonArrayBuilder updated = Json.createArrayBuilder(revisions);
        if (index >= 0) {
            updated.remove(index);
            result = this.storage.save(target, new RevContent(updated.build()).toContent())
                .thenApply(nothing -> true);
        } else {
            result = CompletableFuture.completedFuture(false);
        }
        return result;
    }

    /**
     * Returns index of json element with key == targetValue.
     * @param array Json array to search.
     * @param key Array element key to search.
     * @param value Target value for key to search.
     * @return Index if json array, or -1 if not found.
     */
    private static int jsonIndexOf(final JsonArray array, final String key, final Object value) {
        int index = -1;
        for (int idx = 0; idx < array.size(); ++idx) {
            if (RevisionsIndexCore.getJsonStr(array.get(idx), key).equals(value.toString())) {
                index = idx;
                break;
            }
        }
        return index;
    }
}
