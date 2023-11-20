/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.rq;

import java.net.URI;

/**
 * Request line helper object.
 * <p>
 * See 5.1 section of RFC2616:
 * </p>
 * <p>
 * The Request-Line begins with a method token,
 * followed by the Request-URI and the protocol version,
 * and ending with {@code CRLF}.
 * The elements are separated by SP characters.
 * No {@code CR} or {@code LF} is allowed except in the final {@code CRLF} sequence.
 * </p>
 * <p>
 * {@code Request-Line = Method SP Request-URI SP HTTP-Version CRLF}.
 * </p>
 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html">RFC2616</a>
 * @since 0.1
 */
public final class RequestLineFrom {

    /**
     * HTTP request line.
     */
    private final String line;

    /**
     * Primary ctor.
     * @param line HTTP request line
     */
    public RequestLineFrom(final String line) {
        this.line = line;
    }

    /**
     * Request method.
     * @return Method name
     */
    public RqMethod method() {
        final String string = this.part(0);
        return RqMethod.ALL
            .stream()
            .filter(method -> method.value().equals(string))
            .findAny()
            .orElseThrow(
                () -> new IllegalStateException(String.format("Unknown method: '%s'", string))
            );
    }

    /**
     * Request URI.
     * @return URI of the request
     */
    public URI uri() {
        return URI.create(this.part(1));
    }

    /**
     * HTTP version.
     * @return HTTP version string
     */
    public String version() {
        return this.part(2);
    }

    /**
     * Part of request line. Valid HTTP request line must contains 3 parts which can be
     * splitted by whitespace char.
     * @param idx Part index
     * @return Part string
     */
    private String part(final int idx) {
        final String[] parts = this.line.trim().split("\\s");
        // @checkstyle MagicNumberCheck (1 line)
        if (parts.length == 3) {
            return parts[idx];
        } else {
            throw new IllegalArgumentException(
                String.format("Invalid HTTP request line \n%s", this.line)
            );
        }
    }
}
