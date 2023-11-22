/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.http.Slice;
import java.security.Permission;

/**
 * Slice requiring authorization specified by {@link Scope}.
 *
 * @since 0.11
 */
public interface ScopeSlice extends Slice {

    /**
     * Evaluate authentication scope from HTTP request line.
     *
     * @param line HTTP request line.
     * @param name Repository name
     * @return Scope.
     */
    Permission permission(String line, String name);

}
