/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Resource delegating requests handling to other resources, found by routing path.
 */
public final class RoutingResource implements Resource {

    /**
     * Resource path.
     */
    private final String path;

    /**
     * Routes.
     */
    private final Route[] routes;

    /**
     * Ctor.
     *
     * @param path Resource path.
     * @param routes Routes.
     */
    public RoutingResource(final String path, final Route... routes) {
        this.path = path;
        this.routes = Arrays.copyOf(routes, routes.length);
    }

    @Override
    public Response get(final Headers headers) {
        return this.resource().get(headers);
    }

    @Override
    public Response put(Headers headers, Content body) {
        return this.resource().put(headers, body);
    }

    /**
     * Find resource by path.
     *
     * @return Resource found by path.
     */
    private Resource resource() {
        return Arrays.stream(this.routes)
            .filter(r -> this.path.startsWith(r.path()))
            .max(Comparator.comparing(Route::path))
            .map(r -> r.resource(this.path))
            .orElse(new Absent());
    }

}
