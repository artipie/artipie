/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.perms.DockerRegistryPermission;
import com.artipie.docker.perms.RegistryCategory;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqParams;

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
        public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
            final RqParams params = new RqParams(line.uri().getQuery());
            return this.docker.catalog(
                params.value("last").map(RepoName.Simple::new),
                params.value("n").map(Integer::parseInt).orElse(Integer.MAX_VALUE)
            ).thenApply(
                catalog -> ResponseBuilder.ok()
                    .header(ContentType.json())
                    .body(catalog.json())
                    .build()
            ).toCompletableFuture();
        }
    }
}
