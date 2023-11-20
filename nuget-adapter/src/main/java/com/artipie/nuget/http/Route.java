/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

/**
 * Route that leads to resource.
 *
 * @since 0.1
 */
public interface Route {

    /**
     * Base path for resources.
     * If HTTP request path starts with given path, then this route may be used.
     *
     * @return Path prefix covered by this route.
     */
    String path();

    /**
     * Gets resource by path.
     *
     * @param path Path to resource.
     * @return Resource by path.
     */
    Resource resource(String path);
}
