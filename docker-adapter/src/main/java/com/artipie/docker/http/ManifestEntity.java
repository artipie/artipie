/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.error.ManifestError;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.manifest.ManifestLayer;
import com.artipie.docker.misc.ImageRepositoryName;
import com.artipie.docker.misc.ImageTag;
import com.artipie.docker.misc.RqByRegex;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Location;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLine;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Manifest entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#manifest">Manifest</a>.
 */
final class ManifestEntity {

    /**
     * RegEx pattern for path.
     */
    public static final Pattern PATH = Pattern.compile("^/v2/(?<name>.*)/manifests/(?<reference>.*)$");

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
        public DockerRepositoryPermission permission(final RequestLine line, final String registryName) {
            return new DockerRepositoryPermission(
                registryName, new Request(line).name(), DockerActions.PULL.mask()
            );
        }

        @Override
        public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
            final Request request = new Request(line);
            final ManifestReference ref = request.reference();
            return this.docker.repo(request.name()).manifests()
                .get(ref)
                .thenApply(
                    manifest -> manifest.map(
                        found -> baseResponse(found)
                            .header(new ContentLength(found.size()))
                            .build()
                    ).orElseGet(
                        () -> ResponseBuilder.notFound()
                            .jsonBody(new ManifestError(ref).json())
                            .build()
                    )
                ).toCompletableFuture();
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
        public DockerRepositoryPermission permission(final RequestLine line, final String registryName) {
            return new DockerRepositoryPermission(
                registryName, new Request(line).name(), DockerActions.PULL.mask()
            );
        }

        @Override
        public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
            final Request request = new Request(line);
            final String name = request.name();
            final ManifestReference ref = request.reference();
            return this.docker.repo(name).manifests().get(ref)
                .thenApply(
                    manifest -> manifest.map(
                        found -> baseResponse(found)
                            .body(found.content())
                            .build()
                    ).orElseGet(
                        () -> ResponseBuilder.notFound()
                            .jsonBody(new ManifestError(ref).json())
                            .build()
                )
                ).toCompletableFuture();
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
         * @param docker Docker repository.
         * @param events Artifact events queue
         * @param rname Repository name
         */
        Put(Docker docker, Optional<Queue<ArtifactEvent>> events, String rname) {
            this.docker = docker;
            this.events = events;
            this.rname = rname;
        }

        @Override
        public DockerRepositoryPermission permission(final RequestLine line, final String registryName) {
            return new DockerRepositoryPermission(
                registryName, new Request(line).name(), DockerActions.PUSH.mask()
            );
        }

        @Override
        public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
            final Request request = new Request(line);
            final String name = request.name();
            final ManifestReference ref = request.reference();
            return this.docker.repo(name).manifests()
                .put(ref, new Content.From(body))
                .thenApply(
                    manifest -> {
                        if (this.events.isPresent() && ImageTag.valid(ref.reference())) {
                            this.events.get().add(
                                new ArtifactEvent(
                                    ManifestEntity.REPO_TYPE,
                                    this.rname,
                                    new Login(headers).getValue(),
                                    name, ref.reference(),
                                    manifest.layers().stream().mapToLong(ManifestLayer::size).sum()
                                )
                            );
                        }
                        return ResponseBuilder.created()
                            .header(new Location(String.format("/v2/%s/manifests/%s", name, ref.reference())))
                            .header(new ContentLength("0"))
                            .header(new DigestHeader(manifest.digest()))
                            .build();
                    }
                ).toCompletableFuture();
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
        public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
            final Request request = new Request(line);
            final String name = request.name();
            final ManifestReference ref = request.reference();
            return this.docker.repo(name).manifests().get(ref).thenApply(
                    manifest -> {
                        final OperationControl control;
                        if (manifest.isPresent()) {
                            control = new OperationControl(
                                this.policy, this.permission(line, this.repoName)
                            );
                        } else {
                            final String img = new Request(line).name();
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
                .toCompletableFuture()
                .thenCompose(Function.identity());
        }

        @Override
        public DockerRepositoryPermission permission(final RequestLine line, final String registryName) {
            return new DockerRepositoryPermission(
                registryName, new Request(line).name(), DockerActions.OVERWRITE.mask());
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
        String name() {
            return ImageRepositoryName.validate(this.rqregex.path().group("name"));
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

    private static ResponseBuilder baseResponse(Manifest manifest) {
        return ResponseBuilder.ok()
            .header(ContentType.mime(manifest.mediaType()))
            .header(new DigestHeader(manifest.digest()));
    }
}
