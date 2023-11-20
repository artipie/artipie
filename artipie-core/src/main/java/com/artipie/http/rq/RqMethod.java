/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq;

import java.util.EnumSet;
import java.util.Set;

/**
 * HTTP request method.
 * See <a href="https://tools.ietf.org/html/rfc2616#section-5.1.1">RFC 2616 5.1.1 Method</a>
 *
 * @since 0.4
 */
public enum RqMethod {

    /**
     * OPTIONS.
     */
    OPTIONS("OPTIONS"),

    /**
     * GET.
     */
    GET("GET"),

    /**
     * HEAD.
     */
    HEAD("HEAD"),

    /**
     * POST.
     */
    POST("POST"),

    /**
     * PUT.
     */
    PUT("PUT"),

    /**
     * PATCH.
     */
    PATCH("PATCH"),

    /**
     * DELETE.
     */
    DELETE("DELETE"),

    /**
     * TRACE.
     */
    TRACE("TRACE"),

    /**
     * CONNECT.
     */
    CONNECT("CONNECT");

    /**
     * Set of all existing methods.
     */
    public static final Set<RqMethod> ALL = EnumSet.allOf(RqMethod.class);

    /**
     * String value.
     */
    private final String string;

    /**
     * Ctor.
     *
     * @param string String value.
     */
    RqMethod(final String string) {
        this.string = string;
    }

    /**
     * Method string.
     *
     * @return Method string.
     */
    public String value() {
        return this.string;
    }
}
