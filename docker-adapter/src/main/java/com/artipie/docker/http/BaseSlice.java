/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.perms.DockerRegistryPermission;
import com.artipie.docker.perms.RegistryCategory;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.rq.RequestLine;

import java.util.concurrent.CompletableFuture;

/**
 * Base entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#base">Base</a>.
 */
public final class BaseSlice extends DockerActionSlice {

    public BaseSlice(Docker docker) {
        super(docker);
    }

    @Override
    public DockerRegistryPermission permission(final RequestLine line) {
        return new DockerRegistryPermission(docker.registryName(), RegistryCategory.BASE.mask());
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        return ResponseBuilder.ok()
            .header("Docker-Distribution-API-Version", "registry/2.0")
            .completedFuture();
    }
}
