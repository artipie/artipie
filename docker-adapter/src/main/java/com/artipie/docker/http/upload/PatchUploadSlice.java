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
import com.artipie.http.slice.ContentWithSize;

import java.security.Permission;
import java.util.concurrent.CompletableFuture;

public class PatchUploadSlice extends UploadSlice {

    public PatchUploadSlice(Docker docker) {
        super(docker);
    }

    @Override
    public Permission permission(RequestLine line, String registryName) {
        return new DockerRepositoryPermission(
            registryName, UploadRequest.from(line).name(), DockerActions.PUSH.mask()
        );
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        UploadRequest request = UploadRequest.from(line);
        return this.docker.repo(request.name())
            .uploads()
            .get(request.uuid())
            .thenCompose(
                found -> found.map(
                    upload -> upload
                        .append(new ContentWithSize(body, headers))
                        .thenCompose(offset -> acceptedResponse(request.name(), request.uuid(), offset))
                ).orElseGet(
                    () -> ResponseBuilder.notFound()
                        .jsonBody(new UploadUnknownError(request.uuid()).json())
                        .completedFuture()
                )
            );
    }
}
