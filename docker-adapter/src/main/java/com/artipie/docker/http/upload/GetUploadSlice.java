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
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;

import java.security.Permission;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class GetUploadSlice extends UploadSlice {

    public GetUploadSlice(Docker docker) {
        super(docker);
    }

    @Override
    public Permission permission(RequestLine line) {
        return new DockerRepositoryPermission(
            docker.registry(), UploadRequest.from(line).name(), DockerActions.PULL.mask()
        );
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        UploadRequest request = UploadRequest.from(line);
        return this.docker.repo(request.name())
            .uploads()
            .get(request.uuid())
            .thenApply(
                found -> found.map(
                    upload -> upload.offset()
                        .thenApply(
                            offset -> ResponseBuilder.noContent()
                                .header(new ContentLength("0"))
                                .header(new Header("Range", String.format("0-%d", offset)))
                                .header(new Header("Docker-Upload-UUID", request.uuid()))
                                .build()
                        )
                ).orElseGet(
                    () -> ResponseBuilder.notFound()
                        .jsonBody(new UploadUnknownError(request.uuid()).json())
                        .completedFuture()
                )
            ).thenCompose(Function.identity());
    }
}
