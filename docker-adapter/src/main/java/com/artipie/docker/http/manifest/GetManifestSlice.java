/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http.manifest;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.error.ManifestError;
import com.artipie.docker.http.DigestHeader;
import com.artipie.docker.http.DockerActionSlice;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLine;

import java.security.Permission;
import java.util.concurrent.CompletableFuture;

public class GetManifestSlice extends DockerActionSlice {

    public GetManifestSlice(Docker docker) {
        super(docker);
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        ManifestRequest request = ManifestRequest.from(line);
        return this.docker.repo(request.name())
            .manifests()
            .get(request.reference())
            .thenApply(
                manifest -> manifest.map(
                    found -> ResponseBuilder.ok()
                        .header(ContentType.mime(found.mediaType()))
                        .header(new DigestHeader(found.digest()))
                        .body(found.content())
                        .build()
                ).orElseGet(
                    () -> ResponseBuilder.notFound()
                        .jsonBody(new ManifestError(request.reference()).json())
                        .build()
                )
            );
    }

    @Override
    public Permission permission(RequestLine line) {
        return new DockerRepositoryPermission(
            docker.registry(), ManifestRequest.from(line).name(), DockerActions.PULL.mask()
        );
    }
}
