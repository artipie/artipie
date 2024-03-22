/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.nuget.http.publish;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Login;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.nuget.InvalidPackageException;
import com.artipie.nuget.PackageVersionAlreadyExistsException;
import com.artipie.nuget.Repository;
import com.artipie.nuget.http.Resource;
import com.artipie.nuget.http.Route;
import com.artipie.scheduling.ArtifactEvent;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * Package publish service, used to pushing new packages and deleting existing ones.
 * See <a href="https://docs.microsoft.com/en-us/nuget/api/package-publish-resource">Push and Delete</a>
 */
public final class PackagePublish implements Route {

    /**
     * Repository type constant.
     */
    private static final String REPO_TYPE = "nuget";

    /**
     * Repository for adding package.
     */
    private final Repository repository;

    /**
     * Repository name.
     */
    private final String name;

    /**
     * Artifact events.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Ctor.
     *
     * @param repository Repository for adding package.
     * @param events Repository events queue
     * @param name Repository name
     */
    public PackagePublish(final Repository repository, final Optional<Queue<ArtifactEvent>> events,
        final String name) {
        this.repository = repository;
        this.events = events;
        this.name = name;
    }

    @Override
    public String path() {
        return "/package";
    }

    @Override
    public Resource resource(final String path) {
        return new NewPackage(this.repository, this.events, this.name);
    }

    /**
     * New package resource. Used to push a package into repository.
     * See <a href="https://docs.microsoft.com/en-us/nuget/api/package-publish-resource#push-a-package">Push a package</a>
     */
    public static final class NewPackage implements Resource {

        /**
         * Repository for adding package.
         */
        private final Repository repository;

        /**
         * Repository name.
         */
        private final String name;

        /**
         * Artifact events.
         */
        private final Optional<Queue<ArtifactEvent>> events;

        /**
         * Ctor.
         *
         * @param repository Repository for adding package.
         * @param events Repository events
         * @param name Repository name
         */
        public NewPackage(final Repository repository, final Optional<Queue<ArtifactEvent>> events,
            final String name) {
            this.repository = repository;
            this.events = events;
            this.name = name;
        }

        @Override
        public Response get(final Headers headers) {
            return BaseResponse.methodNotAllowed();
        }

        @Override
        public Response put(Headers headers, Content body) {
            return new AsyncResponse(
                CompletableFuture.supplyAsync(
                    () -> new Multipart(headers, body).first()
                ).thenCompose(this.repository::add).handle(
                    (info, throwable) -> {
                        if (throwable == null) {
                            this.events.ifPresent(
                                queue -> queue.add(
                                    new ArtifactEvent(
                                        PackagePublish.REPO_TYPE, this.name,
                                        new Login(headers).getValue(), info.packageName(),
                                        info.packageVersion(), info.zipSize()
                                    )
                                )
                            );
                            return RsStatus.CREATED;
                        }
                        return toStatus(throwable.getCause());
                    }
                ).thenApply(BaseResponse::from)
            );
        }

        /**
         * Converts throwable to HTTP response status.
         *
         * @param throwable Throwable.
         * @return HTTP response status.
         */
        private static RsStatus toStatus(final Throwable throwable) {
            final RsStatus status;
            if (throwable instanceof InvalidPackageException) {
                status = RsStatus.BAD_REQUEST;
            } else if (throwable instanceof PackageVersionAlreadyExistsException) {
                status = RsStatus.CONFLICT;
            } else {
                status = RsStatus.INTERNAL_ERROR;
            }
            return status;
        }
    }
}
