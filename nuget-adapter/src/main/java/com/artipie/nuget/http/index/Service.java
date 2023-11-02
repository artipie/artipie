/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.http.index;

/**
 * Service that is listed in {@link ServiceIndex}.
 *
 * @since 0.1
 */
public interface Service {

    /**
     * URL to the resource.
     *
     * @return URL to the resource.
     */
    String url();

    /**
     * Service type.
     *
     * @return A string constant representing the resource type.
     */
    String type();
}
