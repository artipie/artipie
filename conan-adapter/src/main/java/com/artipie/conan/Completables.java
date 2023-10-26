/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package  com.artipie.conan;

import io.vavr.Tuple2;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * Encapsulates handling of collections of CompletableFutures.
 * Here CompletableFuture::join() won't block since it's used after CompletableFuture.allOf().
 * @since 0.1
 */
public final class Completables {

    /**
     * Converts List of CompletableFutures with results to CompletableFuture which provides
     * list or array with results.
     * @param <T> Type of the results.
     * @since 0.1
     */
    public static final class JoinList<T> {

        /**
         * CompletableFuture to wait for all items.
         */
        private final CompletableFuture<Void> alls;

        /**
         * List of CompletableFutures to process.
         */
        private final List<CompletableFuture<T>> futures;

        /**
         * Initializes instance with the List of CompletableFutures.
         * @param futures List of CompletableFutures to process.
         */
        @SuppressWarnings("rawtypes")
        public JoinList(final List<CompletableFuture<T>> futures) {
            this.alls = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            this.futures = futures;
        }

        /**
         * Converts to the List of results inside CompletableFuture.
         * @return List of results inside CompletableFuture.
         */
        public CompletableFuture<List<T>> toList() {
            return this.alls.thenApply(
                nothing -> this.futures.stream().map(CompletableFuture::join)
                    .collect(Collectors.toList())
            );
        }

        /**
         * Converts to the array of results inside CompletableFuture.
         * @param generator Array instance generator.
         * @return Array of results inside CompletableFuture.
         */
        public CompletableFuture<T[]> toArray(final IntFunction<T[]> generator) {
            return this.alls.thenApply(
                nothing -> this.futures.stream().map(CompletableFuture::join).toArray(generator)
            );
        }
    }

    /**
     * Converts List of Tuples with CompletableFutures to CompletableFuture providing
     * list of Tuples.
     * @param <K> Key type for the CompletableFuture result value.
     * @param <V> Type of the result value.
     * @since 0.1
     */
    public static final class JoinTuples<K, V> {

        /**
         * CompletableFuture to wait for all items.
         */
        private final CompletableFuture<Void> alls;

        /**
         * List of Tuples with CompletableFuture.
         */
        private final List<Tuple2<K, CompletableFuture<V>>> futures;

        /**
         * Initializes instance with the List of Tuples with CompletableFuture.
         * @param futures List of Tuples with CompletableFuture.
         */
        public JoinTuples(final List<Tuple2<K, CompletableFuture<V>>> futures) {
            this.alls = CompletableFuture.allOf(
                futures.stream().map(Tuple2::_2).toArray(CompletableFuture[]::new)
            );
            this.futures = futures;
        }

        /**
         * Converts to the List of Tuples results inside CompletableFuture.
         * @return List of Tuples inside CompletableFuture.
         */
        public CompletableFuture<List<Tuple2<K, V>>> toTuples() {
            return this.alls.thenApply(
                nothing -> this.futures.stream().map(
                    tuple -> new Tuple2<>(tuple._1(), tuple._2().join())
                ).collect(Collectors.toList())
            );
        }
    }
}
