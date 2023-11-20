/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Request headers.
 * <p>
 * Request header values by name from headers.
 * Usage (assume {@link com.artipie.http.Slice} implementation):
 * </p>
 * <pre><code>
 *  Response response(String line, Iterable&lt;Map.Entry&lt;String, String&gt;&gt; headers,
 *      Flow.Publisher&lt;ByteBuffer&gt; body) {
 *          List&lt;String&gt; values = new RqHeaders(headers, "content-type");
 *          // use these headers
 *  }
 * </code></pre>
 * <p>
 * Header names are case-insensitive, according to
 * <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">RFC2616 SPEC</a>:
 * </p>
 * <p>
 * &gt; Each header field consists of a name followed by a colon (":") and the field value.
 * </p>
 * <p>
 * &gt; Field names are case-insensitive
 * </p>
 * @since 0.4
 */
public final class RqHeaders extends AbstractList<String> {

    /**
     * Origin list.
     */
    private final List<String> origin;

    /**
     * Header values by name.
     * @param headers All headers
     * @param name Header name
     */
    public RqHeaders(final Iterable<Map.Entry<String, String>> headers, final String name) {
        this.origin = StreamSupport.stream(headers.spliterator(), false)
            .filter(entry -> entry.getKey().equalsIgnoreCase(name))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public String get(final int idx) {
        return this.origin.get(idx);
    }

    @Override
    public int size() {
        return this.origin.size();
    }

    /**
     * Single header by name.
     * <p>
     * Use this class to find single header value by name:
     * </p>
     * <pre><code>
     * Text header = new RqHeaders.Single(headers, "content-type");
     * </code></pre>
     * <p>
     * If no headers were found or headers contains more than one value
     * for name {@link IllegalStateException} will be thrown.
     * </p>
     * @since 0.4
     */
    public static final class Single {

        /**
         * All header values.
         */
        private final List<String> headers;

        /**
         * Single header value among other.
         * @param headers All header values
         * @param name Header name
         */
        public Single(final Iterable<Map.Entry<String, String>> headers, final String name) {
            this.headers = new RqHeaders(headers, name);
        }

        /**
         * Single header value as string.
         * @return String represenation
         */
        public String asString() {
            if (this.headers.isEmpty()) {
                throw new IllegalStateException("No headers were found");
            }
            if (this.headers.size() > 1) {
                throw new IllegalStateException("Too many headers were found");
            }
            return this.headers.get(0);
        }
    }
}
