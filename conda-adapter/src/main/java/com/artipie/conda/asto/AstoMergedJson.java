/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.asto;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.asto.streams.StorageValuePipeline;
import com.artipie.conda.meta.MergedJson;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import javax.json.JsonObject;

/**
 * Asto merged json adds packages metadata to repodata index, reading and writing to/from
 * abstract storage.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AstoMergedJson {

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Repodata file key.
     */
    private final Key key;

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param key Repodata file key
     */
    public AstoMergedJson(final Storage asto, final Key key) {
        this.asto = asto;
        this.key = key;
    }

    /**
     * Merges or adds provided new packages items into repodata.json.
     * @param items Items to merge
     * @return Completable operation
     */
    public CompletionStage<Void> merge(final Map<String, JsonObject> items) {
        return new StorageValuePipeline<>(this.asto, this.key).process(
            (opt, out) -> {
                try {
                    final JsonFactory factory = new JsonFactory();
                    new MergedJson.Jackson(
                        factory.createGenerator(out),
                        opt.map(new UncheckedIOFunc<>(factory::createParser))
                    ).merge(items);
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        );
    }
}
