/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.asto;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.conda.meta.MergedJson;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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
        return new StorageValuePipeline<>(this.asto, this.key).processData(
            (opt, out) -> {
                try {
                    final JsonFactory factory = new JsonFactory();
                    final Optional<JsonParser> parser = opt.map(
                        new UncheckedIOFunc<>(factory::createParser)
                    );
                    new MergedJson.Jackson(
                        factory.createGenerator(out),
                        parser
                    ).merge(items);
                    if (parser.isPresent()) {
                        parser.get().close();
                    }
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        );
    }

    /**
     * Processes storage value content as optional input data and
     * saves the result back as output stream.
     *
     * @param <R> Result type
     * @since 1.5
     * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
     */
    private static final class StorageValuePipeline<R> {

        /**
         * Abstract storage.
         */
        private final Storage asto;

        /**
         * Storage item key to read from.
         */
        private final Key read;

        /**
         * Storage item key to write to.
         */
        private final Key write;

        /**
         * Ctor.
         *
         * @param asto Abstract storage
         * @param read Storage item key to read from
         * @param write Storage item key to write to
         */
        StorageValuePipeline(final Storage asto, final Key read, final Key write) {
            this.asto = asto;
            this.read = read;
            this.write = write;
        }

        /**
         * Ctor.
         *
         * @param asto Abstract storage
         * @param key Item key
         */
        StorageValuePipeline(final Storage asto, final Key key) {
            this(asto, key, key);
        }

        /**
         * Process storage item and save it back.
         *
         * @param action Action to perform with storage content if exists and write back as
         *  output stream.
         * @return Completion action
         * @throws ArtipieIOException On Error
         */
        public CompletionStage<Void> processData(
            final BiConsumer<Optional<byte[]>, OutputStream> action
        ) {
            return this.processWithBytesResult(
                (opt, input) -> {
                    action.accept(opt, input);
                    return null;
                }
            ).thenAccept(
                nothing -> {
                }
            );
        }

        /**
         * Process storage item, save it back and return some result.
         *
         * @param action Action to perform with storage content if exists and write back as
         *  output stream.
         * @return Completion action with the result
         * @throws ArtipieIOException On Error
         */
        public CompletionStage<R> processWithBytesResult(
            final BiFunction<Optional<byte[]>, OutputStream, R> action
        ) {
            final AtomicReference<R> res = new AtomicReference<>();
            return this.asto.exists(this.read)
                .thenCompose(
                    exists -> {
                        final CompletionStage<Optional<byte[]>> stage;
                        if (exists) {
                            stage = this.asto.value(this.read)
                                .thenCompose(
                                    content -> new PublisherAs(content).bytes()
                                ).thenApply(bytes -> Optional.of(bytes));
                        } else {
                            stage = CompletableFuture.completedFuture(Optional.empty());
                        }
                        return stage;
                    }
                ).thenCompose(
                    optional -> {
                        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                            res.set(action.apply(optional, output));
                            return this.asto.save(
                                this.write, new Content.From(output.toByteArray())
                            );
                        } catch (final IOException err) {
                            throw new ArtipieIOException(err);
                        }
                    }
                ).thenApply(nothing -> res.get());
        }
    }
}
