/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.misc.RqByRegex;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqParams;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Tags entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#tags">Tags</a>.
 *
 * @since 0.8
 */
final class TagsEntity {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile("^/v2/(?<name>.*)/tags/list$");

    /**
     * Ctor.
     */
    private TagsEntity() {
    }

    /**
     * Slice for GET method, getting tags list.
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
        public DockerRepositoryPermission permission(final String line, final String name) {
            return new DockerRepositoryPermission(name, new Scope.Repository.Pull(name(line)));
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final RqParams params = new RqParams(new RequestLineFrom(line).uri().getQuery());
            return new AsyncResponse(
                this.docker.repo(name(line)).manifests().tags(
                    params.value("last").map(Tag.Valid::new),
                    params.value("n").map(Integer::parseInt).orElse(Integer.MAX_VALUE)
                ).thenApply(
                    tags -> new RsWithBody(
                        new RsWithHeaders(
                            new RsWithStatus(RsStatus.OK),
                            new JsonContentType()
                        ),
                        tags.json()
                    )
                )
            );
        }

        /**
         * Extract repository name from HTTP request line.
         *
         * @param line Request line.
         * @return Repository name.
         */
        private static RepoName.Valid name(final String line) {
            return new RepoName.Valid(new RqByRegex(line, TagsEntity.PATH).path().group("name"));
        }
    }
}
