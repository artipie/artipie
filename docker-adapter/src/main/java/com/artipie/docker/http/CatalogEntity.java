/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.perms.DockerRegistryPermission;
import com.artipie.docker.perms.RegistryCategory;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqParams;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Catalog entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#catalog">Catalog</a>.
 */
final class CatalogEntity {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile("^/v2/_catalog$");

    /**
     * Ctor.
     */
    private CatalogEntity() {
    }

    /**
     * Slice for GET method, getting catalog.
     *
     * @since 0.8
     */
    public static class Get implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
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
        public Response response(
            final RequestLine line,
            final Headers headers,
            final Publisher<ByteBuffer> body
        ) {
            final RqParams params = new RqParams(line.uri().getQuery());
            return new AsyncResponse(
                this.docker.catalog(
                    params.value("last").map(RepoName.Simple::new),
                    params.value("n").map(Integer::parseInt).orElse(Integer.MAX_VALUE)
                ).thenApply(
                    catalog -> new RsWithBody(
                        new RsWithHeaders(
                            new RsWithStatus(RsStatus.OK),
                            new JsonContentType()
                        ),
                        catalog.json()
                    )
                )
            );
        }
    }
}
