/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.http.Headers;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;

/**
 * Route by HTTP methods rule.
 */
public final class MethodRule implements RtRule {

    public static final RtRule GET = new MethodRule(RqMethod.GET);
    public static final RtRule HEAD = new MethodRule(RqMethod.HEAD);
    public static final RtRule POST = new MethodRule(RqMethod.POST);
    public static final RtRule PUT = new MethodRule(RqMethod.PUT);
    public static final RtRule PATCH = new MethodRule(RqMethod.PATCH);
    public static final RtRule DELETE = new MethodRule(RqMethod.DELETE);

    private final RqMethod method;

    private MethodRule(RqMethod method) {
        this.method = method;
    }

    @Override
    public boolean apply(RequestLine line, Headers headers) {
        return this.method == line.method();
    }
}
