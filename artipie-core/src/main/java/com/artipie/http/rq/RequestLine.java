/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq;

/**
 * Http Request Line.
 * <p>
 * See: 5.1 https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
 * @since 0.1
 */
public final class RequestLine {

    /**
     * The request method.
     */
    private final String method;

    /**
     * The request uri.
     */
    private final String uri;

    /**
     * The Http version.
     */
    private final String version;

    /**
     * Ctor.
     *
     * @param method Request method.
     * @param uri Request URI.
     */
    public RequestLine(final RqMethod method, final String uri) {
        this(method.value(), uri);
    }

    /**
     * Ctor.
     *
     * @param method Request method.
     * @param uri Request URI.
     */
    public RequestLine(final String method, final String uri) {
        this(method, uri, "HTTP/1.1");
    }

    /**
     * Ctor.
     * @param method The http method.
     * @param uri The http uri.
     * @param version The http version.
     */
    public RequestLine(final String method, final String uri, final String version) {
        this.method = method;
        this.uri = uri;
        this.version = version;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s\r\n", this.method, this.uri, this.version);
    }
}
