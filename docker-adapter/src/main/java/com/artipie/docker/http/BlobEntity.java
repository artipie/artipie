/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.error.BlobUnknownError;
import com.artipie.docker.misc.RqByRegex;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Blob entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#blob">Blob</a>.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class BlobEntity {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile(
        "^/v2/(?<name>.*)/blobs/(?<digest>(?!(uploads/)).*)$"
    );

    /**
     * Ctor.
     */
    private BlobEntity() {
    }

    /**
     * Slice for GET method.
     *
     * @since 0.2
     */
    static final class Get implements ScopeSlice {

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
            return new DockerRepositoryPermission(
                name, new Scope.Repository.Pull(new Request(line).name())
            );
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Request request = new Request(line);
            final Digest digest = request.digest();
            return new AsyncResponse(
                this.docker.repo(request.name()).layers().get(digest).thenApply(
                    found -> found.<Response>map(
                        blob -> new AsyncResponse(
                            blob.content().thenCompose(
                                content -> content.size()
                                    .<CompletionStage<Long>>map(CompletableFuture::completedFuture)
                                    .orElseGet(blob::size)
                                    .thenApply(
                                        size -> new RsWithBody(
                                            new BaseResponse(digest),
                                            new Content.From(size, content)
                                        )
                                    )
                            )
                        )
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new BlobUnknownError(digest))
                    )
                )
            );
        }
    }

    /**
     * Slice for HEAD method.
     *
     * @since 0.2
     */
    static final class Head implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         */
        Head(final Docker docker) {
            this.docker = docker;
        }

        @Override
        public DockerRepositoryPermission permission(final String line, final String name) {
            return new DockerRepositoryPermission(
                name, new Scope.Repository.Pull(new Request(line).name())
            );
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Request request = new Request(line);
            final Digest digest = request.digest();
            return new AsyncResponse(
                this.docker.repo(request.name()).layers().get(digest).thenApply(
                    found -> found.<Response>map(
                        blob -> new AsyncResponse(
                            blob.size().thenApply(
                                size -> new RsWithHeaders(
                                    new BaseResponse(blob.digest()),
                                    new ContentLength(String.valueOf(size))
                                )
                            )
                        )
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new BlobUnknownError(digest))
                    )
                )
            );
        }
    }

    /**
     * Blob base response.
     *
     * @since 0.2
     */
    private static class BaseResponse extends Response.Wrap {

        /**
         * Ctor.
         *
         * @param digest Blob digest.
         */
        BaseResponse(final Digest digest) {
            super(
                new RsWithHeaders(
                    new RsWithStatus(RsStatus.OK),
                    new DigestHeader(digest),
                    new ContentType("application/octet-stream")
                )
            );
        }
    }

    /**
     * HTTP request to blob entity.
     *
     * @since 0.2
     */
    static final class Request {

        /**
         * HTTP request line.
         */
        private final RqByRegex rqregex;

        /**
         * Ctor.
         *
         * @param line HTTP request line.
         */
        Request(final String line) {
            this.rqregex = new RqByRegex(line, BlobEntity.PATH);
        }

        /**
         * Get repository name.
         *
         * @return Repository name.
         */
        RepoName name() {
            return new RepoName.Valid(this.rqregex.path().group("name"));
        }

        /**
         * Get digest.
         *
         * @return Digest.
         */
        Digest digest() {
            return new Digest.FromString(this.rqregex.path().group("digest"));
        }

    }
}
