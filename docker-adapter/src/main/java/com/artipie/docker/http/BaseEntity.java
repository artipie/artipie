/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.perms.DockerRegistryPermission;
import com.artipie.docker.perms.RegistryCategory;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.ResponseBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Base entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#base">Base</a>.
 */
public final class BaseEntity implements ScopeSlice {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile("^/v2/$");

    @Override
    public DockerRegistryPermission permission(final RequestLine line, final String name) {
        return new DockerRegistryPermission(name, new Scope.Registry(RegistryCategory.BASE));
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        return CompletableFuture.completedFuture(ResponseBuilder.ok()
            .header("Docker-Distribution-API-Version", "registry/2.0")
            .build());
    }
}
