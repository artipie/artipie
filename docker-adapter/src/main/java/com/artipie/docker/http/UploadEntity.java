/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
import com.artipie.docker.error.UploadUnknownError;
import com.artipie.docker.misc.RqByRegex;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Connection;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.headers.Location;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqParams;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.ContentWithSize;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Blob Upload entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#initiate-blob-upload">Initiate Blob Upload</a>
 * and <a href="https://docs.docker.com/registry/spec/api/#blob-upload">Blob Upload</a>.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class UploadEntity {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile(
        "^/v2/(?<name>.*)/blobs/uploads/(?<uuid>[^/]*).*$"
    );

    /**
     * Upload UUID Header.
     */
    private static final String UPLOAD_UUID = "Docker-Upload-UUID";

    /**
     * Ctor.
     */
    private UploadEntity() {
    }

    /**
     * Slice for POST method.
     *
     * @since 0.2
     */
    public static final class Post implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         */
        Post(final Docker docker) {
            this.docker = docker;
        }

        @Override
        public DockerRepositoryPermission permission(final String line, final String name) {
            return new DockerRepositoryPermission(
                name, new Scope.Repository.Push(new Request(line).name())
            );
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Request request = new Request(line);
            final RepoName target = request.name();
            final Optional<Digest> mount = request.mount();
            final Optional<RepoName> from = request.from();
            final Response response;
            if (mount.isPresent() && from.isPresent()) {
                response = this.mount(mount.get(), from.get(), target);
            } else {
                response = this.startUpload(target);
            }
            return response;
        }

        /**
         * Mounts specified blob from source repository to target repository.
         *
         * @param digest Blob digest.
         * @param source Source repository name.
         * @param target Target repository name.
         * @return HTTP response.
         */
        private Response mount(
            final Digest digest,
            final RepoName source,
            final RepoName target
        ) {
            return new AsyncResponse(
                this.docker.repo(source).layers().get(digest).thenCompose(
                    opt -> opt.map(
                        src -> this.docker.repo(target).layers().mount(src)
                            .<Response>thenApply(
                                blob -> new BlobCreatedResponse(target, blob.digest())
                            )
                    ).orElseGet(
                        () -> CompletableFuture.completedFuture(this.startUpload(target))
                    )
                )
            );
        }

        /**
         * Starts new upload in specified repository.
         *
         * @param name Repository name.
         * @return HTTP response.
         */
        private Response startUpload(final RepoName name) {
            return new AsyncResponse(
                this.docker.repo(name).uploads().start().thenApply(
                    upload -> new StatusResponse(name, upload.uuid(), 0)
                )
            );
        }
    }

    /**
     * Slice for PATCH method.
     *
     * @since 0.2
     */
    public static final class Patch implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         */
        Patch(final Docker docker) {
            this.docker = docker;
        }

        @Override
        public DockerRepositoryPermission permission(final String line, final String name) {
            return new DockerRepositoryPermission(
                name, new Scope.Repository.Push(new Request(line).name())
            );
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Request request = new Request(line);
            final RepoName name = request.name();
            final String uuid = request.uuid();
            return new AsyncResponse(
                this.docker.repo(name).uploads().get(uuid).thenApply(
                    found -> found.<Response>map(
                        upload -> new AsyncResponse(
                            upload.append(new ContentWithSize(body, headers)).thenApply(
                                offset -> new StatusResponse(name, uuid, offset)
                            )
                        )
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new UploadUnknownError(uuid))
                    )
                )
            );
        }
    }

    /**
     * Slice for PUT method.
     *
     * @since 0.2
     */
    public static final class Put implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         */
        Put(final Docker docker) {
            this.docker = docker;
        }

        @Override
        public DockerRepositoryPermission permission(final String line, final String name) {
            return new DockerRepositoryPermission(
                name, new Scope.Repository.Push(new Request(line).name())
            );
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Request request = new Request(line);
            final RepoName name = request.name();
            final String uuid = request.uuid();
            final Repo repo = this.docker.repo(name);
            return new AsyncResponse(
                repo.uploads().get(uuid).thenApply(
                    found -> found.<Response>map(
                        upload -> new AsyncResponse(
                            upload.putTo(repo.layers(), request.digest()).thenApply(
                                any -> new BlobCreatedResponse(name, request.digest())
                            )
                        )
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new UploadUnknownError(uuid))
                    )
                )
            );
        }
    }

    /**
     * Slice for GET method.
     *
     * @since 0.3
     */
    public static final class Get implements ScopeSlice {

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
            final RepoName name = request.name();
            final String uuid = request.uuid();
            return new AsyncResponse(
                this.docker.repo(name).uploads().get(uuid).thenApply(
                    found -> found.<Response>map(
                        upload -> new AsyncResponse(
                            upload.offset().thenApply(
                                offset -> new RsWithHeaders(
                                    new RsWithStatus(RsStatus.NO_CONTENT),
                                    new ContentLength("0"),
                                    new Header("Range", String.format("0-%d", offset)),
                                    new Header(UploadEntity.UPLOAD_UUID, uuid)
                                )
                            )
                        )
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new UploadUnknownError(uuid))
                    )
                )
            );
        }
    }

    /**
     * HTTP request to upload blob entity.
     *
     * @since 0.2
     */
    static final class Request {

        /**
         * HTTP request line.
         */
        private final String line;

        /**
         * Ctor.
         *
         * @param line HTTP request line.
         */
        Request(final String line) {
            this.line = line;
        }

        /**
         * Get repository name.
         *
         * @return Repository name.
         */
        RepoName name() {
            return new RepoName.Valid(
                new RqByRegex(this.line, UploadEntity.PATH).path().group("name")
            );
        }

        /**
         * Get upload UUID.
         *
         * @return Upload UUID.
         */
        String uuid() {
            return new RqByRegex(this.line, UploadEntity.PATH).path().group("uuid");
        }

        /**
         * Get "digest" query parameter.
         *
         * @return Digest.
         */
        Digest digest() {
            return this.params().value("digest").map(Digest.FromString::new).orElseThrow(
                () -> new IllegalStateException(String.format("Unexpected query: %s", this.line))
            );
        }

        /**
         * Get "mount" query parameter.
         *
         * @return Digest, empty if parameter does not present in query.
         */
        Optional<Digest> mount() {
            return this.params().value("mount").map(Digest.FromString::new);
        }

        /**
         * Get "from" query parameter.
         *
         * @return Repository name, empty if parameter does not present in the query.
         */
        Optional<RepoName> from() {
            return this.params().value("from").map(RepoName.Valid::new);
        }

        /**
         * Extract request query parameters.
         *
         * @return Request query parameters.
         */
        private RqParams params() {
            return new RqParams(new RequestLineFrom(this.line).uri().getQuery());
        }
    }

    /**
     * Upload blob status HTTP response.
     *
     * @since 0.2
     */
    private static class StatusResponse implements Response {

        /**
         * Repository name.
         */
        private final RepoName name;

        /**
         * Upload UUID.
         */
        private final String uuid;

        /**
         * Current upload offset.
         */
        private final long offset;

        /**
         * Ctor.
         *
         * @param name Repository name.
         * @param uuid Upload UUID.
         * @param offset Current upload offset.
         */
        StatusResponse(final RepoName name, final String uuid, final long offset) {
            this.name = name;
            this.uuid = uuid;
            this.offset = offset;
        }

        @Override
        public CompletionStage<Void> send(final Connection connection) {
            return new RsWithHeaders(
                new RsWithStatus(RsStatus.ACCEPTED),
                new Location(
                    String.format("/v2/%s/blobs/uploads/%s", this.name.value(), this.uuid)
                ),
                new Header("Range", String.format("0-%d", this.offset)),
                new ContentLength("0"),
                new Header(UploadEntity.UPLOAD_UUID, this.uuid)
            ).send(connection);
        }
    }

    /**
     * Blob created response.
     *
     * @since 0.9
     */
    private static final class BlobCreatedResponse extends Response.Wrap {

        /**
         * Ctor.
         *
         * @param name Repository name.
         * @param digest Blob digest.
         */
        private BlobCreatedResponse(final RepoName name, final Digest digest) {
            super(
                new RsWithHeaders(
                    new RsWithStatus(RsStatus.CREATED),
                    new Location(String.format("/v2/%s/blobs/%s", name.value(), digest.string())),
                    new ContentLength("0"),
                    new DigestHeader(digest)
                )
            );
        }
    }

    /**
     * Slice for DELETE method.
     *
     * @since 0.16
     */
    public static final class Delete implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         */
        Delete(final Docker docker) {
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
            final RepoName name = request.name();
            final String uuid = request.uuid();
            return new AsyncResponse(
                this.docker.repo(name).uploads().get(uuid).thenCompose(
                    x -> x.map(
                        (Function<Upload, CompletionStage<? extends Response>>) upload ->
                            upload.cancel().thenApply(
                                offset -> new RsWithHeaders(
                                    new RsWithStatus(RsStatus.OK),
                                    new Header(UploadEntity.UPLOAD_UUID, uuid)
                                )
                            )
                    ).orElse(
                        CompletableFuture.completedFuture(
                            new ErrorsResponse(RsStatus.NOT_FOUND, new UploadUnknownError(uuid))
                        )
                    )
                )
            );
        }
    }
}
