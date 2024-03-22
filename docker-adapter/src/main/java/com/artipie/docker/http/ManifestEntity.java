/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.error.ManifestError;
import com.artipie.docker.manifest.Layer;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.misc.RqByRegex;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Location;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;

import java.util.Optional;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 * Manifest entity in Docker HTTP API..
 * See <a href="https://docs.docker.com/registry/spec/api/#manifest">Manifest</a>.
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

    private ManifestEntity() {
    }

    /**
     * Slice for HEAD method, checking manifest existence.
     */
    public static class Head implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;

        /**
         * @param docker Docker repository.
         */
        Head(final Docker docker) {
            this.docker = docker;
        }

        @Override
        public DockerRepositoryPermission permission(final RequestLine line, final String name) {
            return new DockerRepositoryPermission(
                name, new Scope.Repository.Pull(new Request(line).name())
            );
        }

        @Override
        public Response response(RequestLine line, Headers headers, Content body) {
            final Request request = new Request(line);
            final ManifestReference ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(request.name()).manifests().get(ref).thenApply(
                    manifest -> manifest.<Response>map(
                        found -> baseResponse(found).header(new ContentLength(found.size()))
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new ManifestError(ref))
                    )
                )
            );
        }
    }

    /**
     * Slice for GET method, getting manifest content.
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
        public DockerRepositoryPermission permission(final RequestLine line, final String name) {
            return new DockerRepositoryPermission(
                name, new Scope.Repository.Pull(new Request(line).name())
            );
        }

        @Override
        public Response response(RequestLine line, Headers headers, Content body) {
            final Request request = new Request(line);
            final RepoName name = request.name();
            final ManifestReference ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(name).manifests().get(ref).thenApply(
                    manifest -> manifest.<Response>map(
                        found -> baseResponse(found).body(found.content())
                    ).orElseGet(
                        () -> new ErrorsResponse(RsStatus.NOT_FOUND, new ManifestError(ref))
                    )
                )
            );
        }

    }

    /**
     * Slice for PUT method, uploading manifest content.
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
        public DockerRepositoryPermission permission(final RequestLine line, final String name) {
            return new DockerRepositoryPermission(
                name, new Scope.Repository.Push(new Request(line).name())
            );
        }

        @Override
        public Response response(RequestLine line, Headers headers, Content body) {
            final Request request = new Request(line);
            final RepoName name = request.name();
            final ManifestReference ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(name).manifests().put(ref, new Content.From(body)).thenApply(
                    manifest -> {
                        if (this.events.isPresent() && new Tag.Valid(ref.reference()).valid()) {
                            this.events.get().add(
                                new ArtifactEvent(
                                    ManifestEntity.REPO_TYPE, this.rname,
                                    new Login(headers).getValue(),
                                    name.value(), ref.reference(),
                                    manifest.layers().stream().mapToLong(Layer::size).sum()
                                )
                            );
                        }
                        return BaseResponse.created()
                            .header(new Location(String.format("/v2/%s/manifests/%s", name.value(), ref.reference())))
                            .header(new ContentLength("0"))
                            .header(new DigestHeader(manifest.digest()));
                    }
                )
            );
        }
    }

    /**
     * Auth slice for PUT method, checks whether overwrite is allowed.
     */
    public static class PutAuth implements ScopeSlice {

        /**
         * Docker repository.
         */
        private final Docker docker;
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
        private final String repoName;

        /**
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
            this.repoName = name;
        }

        @Override
        public Response response(RequestLine line, Headers headers, Content body) {
            final Request request = new Request(line);
            final RepoName name = request.name();
            final ManifestReference ref = request.reference();
            return new AsyncResponse(
                this.docker.repo(name).manifests().get(ref).thenApply(
                    manifest -> {
                        final OperationControl control;
                        if (manifest.isPresent()) {
                            control = new OperationControl(
                                this.policy, this.permission(line, this.repoName)
                            );
                        } else {
                            final String img = new Request(line).name().value();
                            control = new OperationControl(
                                this.policy,
                                new DockerRepositoryPermission(
                                    this.repoName, img,
                                    DockerActions.PUSH.mask()
                                ),
                                new DockerRepositoryPermission(
                                    this.repoName, img,
                                    DockerActions.OVERWRITE.mask()
                                )
                            );
                        }
                        return new DockerAuthSlice(
                            new AuthzSlice(this.origin, this.auth, control)
                        ).response(line, headers, body);
                    }
                )
            );
        }

        @Override
        public DockerRepositoryPermission permission(final RequestLine line, final String name) {
            return new DockerRepositoryPermission(
                name,
                new Scope.Repository.OverwriteTags(new Request(line).name())
            );
        }
    }

    /**
     * HTTP request to manifest entity.
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
        Request(final RequestLine line) {
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
        ManifestReference reference() {
            return ManifestReference.from(this.rqregex.path().group("reference"));
        }

    }

    private static BaseResponse baseResponse(Manifest manifest) {
        return BaseResponse.ok()
            .header(ContentType.mime(manifest.mediaType()))
            .header(new DigestHeader(manifest.digest()));
    }
}
