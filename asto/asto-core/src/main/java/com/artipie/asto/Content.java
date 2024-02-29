/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.ext.PublisherAs;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

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
     * Provides size of content in bytes if known.
     *
     * @return Size of content in bytes if known.
     */
    Optional<Long> size();

    /**
     * Reads bytes from content into memory.
     *
     * @return Byte array as CompletionStage
     */
    default CompletableFuture<byte[]> asBytesFuture() {
        return new PublisherAs(this).bytes().toCompletableFuture();
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
     * Reads bytes from content as string in the {@code StandardCharsets.UTF_8} charset.
     *
     * @return String as CompletionStage
     */
    default CompletableFuture<String> asStringFuture() {
        return new PublisherAs(this).string(StandardCharsets.UTF_8).toCompletableFuture();
    }

    /**
     * Reads bytes from content as string in the {@code StandardCharsets.UTF_8} charset.
     *
     * @return String
     */
    default String asString() {
        return this.asStringFuture().join();
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
         * Ctor.
         *
         * @param publisher Content bytes.
         */
        public From(final Publisher<ByteBuffer> publisher) {
            this(Optional.empty(), publisher);
        }

        /**
         * Ctor.
         *
         * @param size Total content size in bytes.
         * @param publisher Content bytes.
         */
        public From(final long size, final Publisher<ByteBuffer> publisher) {
            this(Optional.of(size), publisher);
        }

        /**
         * Ctor.
         *
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
