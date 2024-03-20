/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Content that can be stored in {@link Storage}.
 */
public interface Content extends Publisher<ByteBuffer> {

    /**
     * Empty content.
     */
    Content EMPTY = new Empty();

    /**
     * Provides size of the content in bytes if known.
     *
     * @return Size of content in bytes if known.
     */
    Optional<Long> size();

    /**
     * Reads bytes from the content into memory.
     *
     * @return Byte array as CompletableFuture
     */
    default CompletableFuture<byte[]> asBytesFuture() {
        return new Concatenation(this)
            .single()
            .map(buf -> new Remaining(buf, true))
            .map(Remaining::bytes)
            .to(SingleInterop.get())
            .toCompletableFuture();
    }

    /**
     * Reads bytes from content into memory.
     *
     * @return Byte array
     */
    default byte[] asBytes() {
        return this.asBytesFuture().join();
    }

    /**
     * Reads bytes from the content as a string in the {@code StandardCharsets.UTF_8} charset.
     *
     * @return String as CompletableFuture
     */
    default CompletableFuture<String> asStringFuture() {
        return this.asBytesFuture().thenApply(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Reads bytes from the content as a string in the {@code StandardCharsets.UTF_8} charset.
     *
     * @return String
     */
    default String asString() {
        return this.asStringFuture().join();
    }

    /**
     * Reads bytes from the content as a JSON object.
     *
     * @return JsonObject as CompletableFuture
     */
    default CompletableFuture<JsonObject> asJsonObjectFuture() {
        return this.asStringFuture().thenApply(val -> {
            try (JsonReader reader = Json.createReader(new StringReader(val))) {
                return reader.readObject();
            }
        });
    }

    /**
     * Reads bytes from the content as a JSON object.
     *
     * @return JsonObject
     */
    default JsonObject asJsonObject() {
        return this.asJsonObjectFuture().join();
    }

    /**
     * Empty content.
     */
    final class Empty implements Content {

        @Override
        public Optional<Long> size() {
            return Optional.of(0L);
        }

        @Override
        public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
            Flowable.<ByteBuffer>empty().subscribe(subscriber);
        }
    }

    /**
     * Key built from byte buffers publisher and total size if it is known.
     */
    final class From implements Content {

        /**
         * Total content size in bytes, if known.
         */
        private final Optional<Long> length;

        /**
         * Content bytes.
         */
        private final Publisher<ByteBuffer> publisher;

        /**
         * Ctor.
         *
         * @param array Content bytes.
         */
        public From(final byte[] array) {
            this(
                array.length,
                Flowable.fromArray(ByteBuffer.wrap(Arrays.copyOf(array, array.length)))
            );
        }

        /**
         * @param publisher Content bytes.
         */
        public From(final Publisher<ByteBuffer> publisher) {
            this(Optional.empty(), publisher);
        }

        /**
         * @param size Total content size in bytes.
         * @param publisher Content bytes.
         */
        public From(final long size, final Publisher<ByteBuffer> publisher) {
            this(Optional.of(size), publisher);
        }

        /**
         * @param size Total content size in bytes, if known.
         * @param publisher Content bytes.
         */
        public From(final Optional<Long> size, final Publisher<ByteBuffer> publisher) {
            this.length = size;
            this.publisher = publisher;
        }

        @Override
        public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
            this.publisher.subscribe(subscriber);
        }

        @Override
        public Optional<Long> size() {
            return this.length;
        }
    }

    /**
     * A content which can be consumed only once.
     *
     * @since 0.24
     */
    final class OneTime implements Content {

        /**
         * The wrapped content.
         */
        private final Content wrapped;

        /**
         * Ctor.
         *
         * @param original The original content
         */
        public OneTime(final Content original) {
            this.wrapped = new Content.From(original.size(), new OneTimePublisher<>(original));
        }

        @Override
        public Optional<Long> size() {
            return this.wrapped.size();
        }

        @Override
        public void subscribe(final Subscriber<? super ByteBuffer> sub) {
            this.wrapped.subscribe(sub);
        }
    }
}
