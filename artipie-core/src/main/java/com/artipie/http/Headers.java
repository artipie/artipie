/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.headers.Header;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * HTTP request headers.
 *
 * @since 0.8
 * @checkstyle InterfaceIsTypeCheck (2 lines)
 */
public interface Headers extends Iterable<Map.Entry<String, String>> {

    /**
     * Empty headers.
     */
    Headers EMPTY = new From(Collections.emptyList());

    /**
     * {@link Headers} created from something.
     *
     * @since 0.8
     */
    final class From implements Headers {

        /**
         * Origin headers.
         */
        private final Iterable<Map.Entry<String, String>> origin;

        /**
         * Ctor.
         *
         * @param name Header name.
         * @param value Header value.
         */
        public From(final String name, final String value) {
            this(new Header(name, value));
        }

        /**
         * Ctor.
         *
         * @param origin Origin headers.
         * @param name Additional header name.
         * @param value Additional header value.
         */
        public From(
            final Iterable<Map.Entry<String, String>> origin,
            final String name, final String value
        ) {
            this(origin, new Header(name, value));
        }

        /**
         * Ctor.
         *
         * @param header Header.
         */
        public From(final Map.Entry<String, String> header) {
            this(Collections.singleton(header));
        }

        /**
         * Ctor.
         *
         * @param origin Origin headers.
         * @param additional Additional headers.
         */
        public From(
            final Iterable<Map.Entry<String, String>> origin,
            final Map.Entry<String, String> additional
        ) {
            this(origin, Collections.singleton(additional));
        }

        /**
         * Ctor.
         *
         * @param origin Origin headers.
         */
        @SafeVarargs
        public From(final Map.Entry<String, String>... origin) {
            this(Arrays.asList(origin));
        }

        /**
         * Ctor.
         *
         * @param origin Origin headers.
         * @param additional Additional headers.
         */
        @SafeVarargs
        public From(
            final Iterable<Map.Entry<String, String>> origin,
            final Map.Entry<String, String>... additional
        ) {
            this(origin, Arrays.asList(additional));
        }

        /**
         * Ctor.
         *
         * @param origin Origin headers.
         * @param additional Additional headers.
         */
        public From(
            final Iterable<Map.Entry<String, String>> origin,
            final Iterable<Map.Entry<String, String>> additional
        ) {
            this(Iterables.concat(origin, additional));
        }

        /**
         * Ctor.
         *
         * @param origin Origin headers.
         */
        public From(final Iterable<Map.Entry<String, String>> origin) {
            this.origin = origin;
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return this.origin.iterator();
        }

        @Override
        public void forEach(final Consumer<? super Map.Entry<String, String>> action) {
            this.origin.forEach(action);
        }

        @Override
        public Spliterator<Map.Entry<String, String>> spliterator() {
            return this.origin.spliterator();
        }
    }

    /**
     * Abstract decorator for {@link Headers}.
     * @since 0.10
     */
    abstract class Wrap implements Headers {

        /**
         * Origin headers.
         */
        private final Iterable<Map.Entry<String, String>> origin;

        /**
         * Ctor.
         * @param origin Origin headers
         */
        protected Wrap(final Iterable<Map.Entry<String, String>> origin) {
            this.origin = origin;
        }

        /**
         * Ctor.
         * @param origin Origin headers
         */
        protected Wrap(final Header... origin) {
            this(Arrays.asList(origin));
        }

        @Override
        public final Iterator<Map.Entry<String, String>> iterator() {
            return this.origin.iterator();
        }

        @Override
        public final void forEach(final Consumer<? super Map.Entry<String, String>> action) {
            this.origin.forEach(action);
        }

        @Override
        public final Spliterator<Map.Entry<String, String>> spliterator() {
            return this.origin.spliterator();
        }
    }
}
