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

    Permission permission(RequestLine line, String registryName);

}
