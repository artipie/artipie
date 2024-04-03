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
import java.util.regex.Pattern;

/**
 * Catalog entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#catalog">Catalog</a>.
 */
final class CatalogEntity {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile("^/v2/_catalog$");

    private CatalogEntity() {
        // No-op.
    }

    /**
     * Slice for GET method, getting catalog.
     */
    public static class Get implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * @param docker Docker repository.
         */
        Get(final Docker docker) {
            this.docker = docker;
        }

        @Override
        public DockerRegistryPermission permission(final RequestLine line, final String name) {
            return new DockerRegistryPermission(name, new Scope.Registry(RegistryCategory.CATALOG));
        }

        @Override
        public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
            return this.docker.catalog(Pagination.from(line.uri()))
                .thenApply(catalog -> ResponseBuilder.ok()
                    .header(ContentType.json())
                    .body(catalog.json())
                    .build());
        }
    }
}
