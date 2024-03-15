/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rq;

import java.net.URI;
import java.util.Objects;

/**
 * Request line helper object.
 * <p>
 * See 5.1 section of RFC2616:
 * <p>
 * The Request-Line begins with a method token,
 * followed by the Request-URI and the protocol version,
 * and ending with {@code CRLF}.
 * The elements are separated by SP characters.
 * No {@code CR} or {@code LF} is allowed except in the final {@code CRLF} sequence.
 * <p>
 *     {@code Request-Line = Method SP Request-URI SP HTTP-Version CRLF}.
 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html">RFC2616</a>
 */
public final class RequestLine {

    public static RequestLine from(String line) {
        RequestLineFrom from = new RequestLineFrom(line);
        return new RequestLine(from.method(), from.uri(), from.version());
    }

    /**
     * The request method.
     */
    private final RqMethod method;

    /**
     * The request uri.
     */
    private final URI uri;

    /**
     * The Http version.
     */
    private final String version;

    /**
     * @param method Request method.
     * @param uri Request URI.
     */
    public RequestLine(final RqMethod method, final String uri) {
        this(method.value(), uri);
    }

    /**
     * @param method Request method.
     * @param uri Request URI.
     */
    public RequestLine(String method, String uri) {
        this(method, uri, "HTTP/1.1");
    }

    /**
     * @param method The http method.
     * @param uri The http uri.
     * @param version The http version.
     */
    public RequestLine(String method, String uri, String version) {
        this(RqMethod.valueOf(method), URI.create(uri), version);
    }

    public RequestLine(RqMethod method, URI uri, String version) {
        this.method = method;
        this.uri = uri;
        this.version = version;
    }

    public RqMethod method() {
        return method;
    }

    public URI uri() {
        return uri;
    }

    public String version() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestLine that = (RequestLine) o;
        return method == that.method && Objects.equals(uri, that.uri) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, uri, version);
    }

    @Override
    public String toString() {
        return this.method.value() + ' ' + this.uri + ' ' + this.version;
    }
}
