/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;

/**
 * Route supporting basic authentication.
 *
 * @since 0.2
 */
final class BasicAuthRoute implements Route {

    /**
     * Origin route.
     */
    private final Route origin;

    /**
     * Operation access control.
     */
    private final OperationControl control;

    /**
     * Authentication.
     */
    private final Authentication auth;

    /**
     * Ctor.
     *
     * @param origin Origin route.
     * @param control Operation access control.
     * @param auth Authentication mechanism.
     */
    BasicAuthRoute(final Route origin, final OperationControl control, final Authentication auth) {
        this.origin = origin;
        this.auth = auth;
        this.control = control;
    }

    @Override
    public String path() {
        return this.origin.path();
    }

    @Override
    public Resource resource(final String path) {
        return new ResourceFromSlice(
            path,
            new BasicAuthzSlice(
                new SliceFromResource(this.origin.resource(path)),
                this.auth,
                this.control
            )
        );
    }
}
