/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Storage content metadata.
 * @since 1.9
 */
public interface Meta {

    /**
     * Operator for MD5 hash.
     */
    OpRWSimple<String> OP_MD5 = new OpRWSimple<>("md5", Function.identity());

    /**
     * Operator for size.
     */
    OpRWSimple<Long> OP_SIZE = new OpRWSimple<>("size", Long::parseLong);

    /**
     * Operator for created time.
     */
    OpRWSimple<Instant> OP_CREATED_AT = new OpRWSimple<>("created-at", Instant::parse);

    /**
     * Operator for updated time.
     */
    OpRWSimple<Instant> OP_UPDATED_AT = new OpRWSimple<>("updated-at", Instant::parse);

    /**
     * Operator for last access time.
     */
    OpRWSimple<Instant> OP_ACCESSED_AT = new OpRWSimple<>("accessed-at", Instant::parse);

    /**
     * Empty metadata.
     */
    Meta EMPTY = new Meta() {
        @Override
        public <T> T read(final Meta.ReadOperator<T> opr) {
            return opr.take(Collections.emptyMap());
        }
    };

    /**
     * Read metadata.
     * @param opr Operator to read
     * @param <T> Value type
     * @return Metadata value
     */
    <T> T read(Meta.ReadOperator<T> opr);

    /**
     * Metadata read operator.
     * @param <T> Result type
     * @since 1.0
     */
    @FunctionalInterface
    interface ReadOperator<T> {

        /**
         * Take metadata param from raw metadata.
         * @param raw Readonly map of strings
         * @return Metadata value
         */
        T take(Map<String, ? extends String> raw);
    }

    /**
     * Metadata write operator.
     * @param <T> Value type
     * @since 1.9
     */
    @FunctionalInterface
    interface WriteOperator<T> {

        /**
         * Put value to raw metadata.
         * @param raw Raw metadata map
         * @param val Value
         */
        void put(Map<String, String> raw, T val);
    }

    /**
     * Read and write simple operator implementation.
     * @param <T> Value type
     * @since 1.9
         */
    final class OpRWSimple<T> implements ReadOperator<Optional<? extends T>>, WriteOperator<T> {

        /**
         * Raw data key.
         */
        private final String key;

        /**
         * Parser function.
         */
        private final Function<? super String, ? extends T> parser;

        /**
         * Serializer func.
         */
        private final Function<? super T, ? extends String> serializer;

        /**
         * New operator with default {@link Object#toString()} serializer.
         * @param key Data key
         * @param parser Parser function
         */
        public OpRWSimple(final String key, final Function<? super String, ? extends T> parser) {
            this(key, parser, Object::toString);
        }

        /**
         * New operator.
         * @param key Data key
         * @param parser Parser function
         * @param serializer Serializer
         */
        public OpRWSimple(final String key, final Function<? super String, ? extends T> parser,
            final Function<? super T, ? extends String> serializer) {
            this.key = key;
            this.parser = parser;
            this.serializer = serializer;
        }

        @Override
        public void put(final Map<String, String> raw, final T val) {
            raw.put(this.key, this.serializer.apply(val));
        }

        @Override
        public Optional<? extends T> take(final Map<String, ? extends String> raw) {
            final Optional<? extends T> result;
            if (raw.containsKey(this.key)) {
                result = Optional.of(raw.get(this.key)).map(this.parser);
            } else {
                result = Optional.empty();
            }
            return result;
        }
    }
}
