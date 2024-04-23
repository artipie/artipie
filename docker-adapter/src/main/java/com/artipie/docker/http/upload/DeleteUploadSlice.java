/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http.upload;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.error.UploadUnknownError;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.rq.RequestLine;

import java.security.Permission;
import java.util.concurrent.CompletableFuture;

public class DeleteUploadSlice extends UploadSlice {

    public DeleteUploadSlice(Docker docker) {
        super(docker);
    }

    @Override
    public Permission permission(RequestLine line) {
        return new DockerRepositoryPermission(
            docker.registryName(), UploadRequest.from(line).name(), DockerActions.PULL.mask()
        );
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        UploadRequest request = UploadRequest.from(line);
        return this.docker.repo(request.name())
            .uploads()
            .get(request.uuid())
            .thenCompose(
                x -> x.map(
                    upload -> upload.cancel()
                        .thenApply(
                            offset -> ResponseBuilder.ok()
                                .header("Docker-Upload-UUID", request.uuid())
                                .build()
                        )
                ).orElse(
                    ResponseBuilder.notFound()
                        .jsonBody(new UploadUnknownError(request.uuid()).json())
                        .completedFuture()
                )
            );
    }
}
