/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.misc.Pagination;
import com.artipie.docker.perms.DockerRegistryPermission;
import com.artipie.docker.perms.RegistryCategory;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLine;

import java.util.concurrent.CompletableFuture;

/**
 * Catalog entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#catalog">Catalog</a>.
 */
public final class CatalogSlice extends DockerActionSlice {

    public CatalogSlice(Docker docker) {
        super(docker);
    }

    @Override
    public DockerRegistryPermission permission(RequestLine line) {
        return new DockerRegistryPermission(docker.registryName(), RegistryCategory.CATALOG.mask());
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        return this.docker.catalog(Pagination.from(line.uri()))
            .thenApply(
                catalog -> ResponseBuilder.ok()
                    .header(ContentType.json())
                    .body(catalog.json())
                    .build()
            );
    }
}
