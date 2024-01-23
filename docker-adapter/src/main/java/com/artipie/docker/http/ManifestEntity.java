/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.error.ManifestError;
import com.artipie.docker.manifest.Layer;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.misc.RqByRegex;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.docker.ref.ManifestRef;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.headers.Accept;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Location;
import com.artipie.http.headers.Login;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Manifest entity in Docker HTTP API..
 * See <a href="https://docs.docker.com/registry/spec/api/#manifest">Manifest</a>.
 *
 * @since 0.2
 */
final class ManifestEntity {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile(
        "^/v2/(?<name>.*)/manifests/(?<reference>.*)$"
    );

    /**
     * Repository type.
     */
    private static final String REPO_TYPE = "docker";

    /**
     * Ctor.
     */
    private ManifestEntity() {
    }

    /**
     * Slice for HEAD method, checking manifest existence.
     *
     * @since 0.2
     */
    public static class Head implements ScopeSlice {

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
            final Publisher<ByteBuffer> body) {
            final Request request = new Request(line);
            final ManifestRef ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(request.name()).manifests().get(ref).thenApply(
                    manifest -> manifest.<Response>map(
                        found -> new RsWithHeaders(
                            new BaseResponse(
                                found.convert(new HashSet<>(new Accept(headers).values()))
                            ),
                            new ContentLength(found.size())
                        )
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new ManifestError(ref))
                    )
                )
            );
        }
    }

    /**
     * Slice for GET method, getting manifest content.
     *
     * @since 0.2
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
            final ManifestRef ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(name).manifests().get(ref).thenApply(
                    manifest -> manifest.<Response>map(
                        found -> {
                            final Manifest mnf = found.convert(
                                new HashSet<>(new Accept(headers).values())
                            );
                            return new RsWithBody(new BaseResponse(mnf), mnf.content());
                        }
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new ManifestError(ref))
                    )
                )
            );
        }

    }

    /**
     * Slice for PUT method, uploading manifest content.
     *
     * @since 0.2
     */
    public static class Put implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Events queue.
         */
        private final Optional<Queue<ArtifactEvent>> events;

        /**
         * Repository name.
         */
        private final String rname;

        /**
         * Ctor.
         *
         * @param docker Docker repository.
         * @param events Artifact events queue
         * @param rname Repository name
         */
        Put(final Docker docker, final Optional<Queue<ArtifactEvent>> events, final String rname) {
            this.docker = docker;
            this.events = events;
            this.rname = rname;
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
            final ManifestRef ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(name).manifests().put(ref, new Content.From(body)).thenApply(
                    manifest -> {
                        if (this.events.isPresent() && new Tag.Valid(ref.string()).valid()) {
                            this.events.get().add(
                                new ArtifactEvent(
                                    ManifestEntity.REPO_TYPE, this.rname,
                                    new Login(new Headers.From(headers)).getValue(),
                                    name.value(), ref.string(),
                                    manifest.layers().stream().mapToLong(Layer::size).sum()
                                )
                            );
                        }
                        return new RsWithHeaders(
                            new RsWithStatus(RsStatus.CREATED),
                            new Location(
                                String.format("/v2/%s/manifests/%s", name.value(), ref.string())
                            ),
                            new ContentLength("0"),
                            new DigestHeader(manifest.digest())
                        );
                    }
                )
            );
        }
    }

    /**
     * Auth slice for PUT method, checks whether overwrite is allowed.
     *
     * @since 0.12
     */
    public static class PutAuth implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * Origin.
         */
        private final ScopeSlice origin;

        /**
         * Access permissions.
         */
        private final Policy<?> policy;

        /**
         * Authentication scheme.
         */
        private final AuthScheme auth;

        /**
         * Artipie repository name.
         */
        private final String rname;

        /**
         * Ctor.
         * @param docker Docker
         * @param origin Origin slice
         * @param auth Authentication
         * @param policy Security policy
         * @param name Artipie repository name
                 */
        PutAuth(final Docker docker, final ScopeSlice origin,
            final AuthScheme auth, final Policy<?> policy, final String name) {
            this.docker = docker;
            this.origin = origin;
            this.policy = policy;
            this.auth = auth;
            this.rname = name;
        }

        @Override
        public Response response(
            final String line, final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Request request = new Request(line);
            final RepoName name = request.name();
            final ManifestRef ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(name).manifests().get(ref).thenApply(
                    manifest -> {
                        final OperationControl control;
                        if (manifest.isPresent()) {
                            control = new OperationControl(
                                this.policy, this.permission(line, this.rname)
                            );
                        } else {
                            control = new OperationControl(
                                this.policy,
                                new DockerRepositoryPermission(
                                    this.rname, new Request(line).name().value(),
                                    DockerActions.PUSH.mask() & DockerActions.OVERWRITE.mask()
                                )
                            );
                        }
                        return new DockerAuthSlice(
                            new AuthzSlice(
                                this.origin,
                                this.auth,
                                control
                            )
                        ).response(line, headers, body);
                    }
                )
            );
        }

        @Override
        public DockerRepositoryPermission permission(final String line, final String name) {
            return new DockerRepositoryPermission(
                name,
                new Scope.Repository.OverwriteTags(new Request(line).name())
            );
        }
    }

    /**
     * HTTP request to manifest entity.
     *
     * @since 0.2
     */
    static final class Request {

        /**
         * HTTP request by RegEx.
         */
        private final RqByRegex rqregex;

        /**
         * Ctor.
         *
         * @param line HTTP request line.
         */
        Request(final String line) {
            this.rqregex = new RqByRegex(line, ManifestEntity.PATH);
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
         * Get manifest reference.
         *
         * @return Manifest reference.
         */
        ManifestRef reference() {
            return new ManifestRef.FromString(this.rqregex.path().group("reference"));
        }

    }

    /**
     * Manifest base response.
     * @since 0.2
     */
    static final class BaseResponse extends Response.Wrap {

        /**
         * Ctor.
         *
         * @param mnf Manifest
         */
        BaseResponse(final Manifest mnf) {
            super(
                new RsWithHeaders(
                    StandardRs.EMPTY,
                    new ContentType(String.join(",", mnf.mediaTypes())),
                    new DigestHeader(mnf.digest())
                )
            );
        }

    }
}
