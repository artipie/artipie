/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http.blobs;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.error.BlobUnknownError;
import com.artipie.docker.http.DigestHeader;
import com.artipie.docker.http.DockerActionSlice;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLine;

import java.security.Permission;
import java.util.concurrent.CompletableFuture;

public class HeadBlobsSlice extends DockerActionSlice {
    public HeadBlobsSlice(Docker docker) {
        super(docker);
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        BlobsRequest request = BlobsRequest.from(line);
        return this.docker.repo(request.name()).layers()
            .get(request.digest())
            .thenCompose(
                found -> found.map(
                    blob -> blob.size().thenApply(
                        size -> ResponseBuilder.ok()
                            .header(new DigestHeader(blob.digest()))
                            .header(ContentType.mime("application/octet-stream"))
                            .header(new ContentLength(size))
                            .build()
                    )
                ).orElseGet(
                    () -> ResponseBuilder.notFound()
                        .jsonBody(new BlobUnknownError(request.digest()).json())
                        .completedFuture()
                )
            );
    }

    @Override
    public Permission permission(RequestLine line) {
        return new DockerRepositoryPermission(
            docker.registryName(), BlobsRequest.from(line).name(), DockerActions.PULL.mask()
        );
    }
}
