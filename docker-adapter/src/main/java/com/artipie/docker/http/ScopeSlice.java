/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.security.Permission;

/**
 * Slice requiring authorization specified by {@link Scope}.
 */
public interface ScopeSlice extends Slice {

    /**
     * Evaluate authentication scope from HTTP request line.
     *
     * @param line HTTP request line.
     * @param name Repository name
     * @return Scope.
     */
    @Deprecated
    default Permission permission(String line, String name){
        return permission(RequestLine.from(line), name);
    }

    Permission permission(RequestLine line, String name);

}
