/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http.manifest;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.http.DigestHeader;
import com.artipie.docker.http.DockerActionSlice;
import com.artipie.docker.manifest.ManifestLayer;
import com.artipie.docker.misc.ImageTag;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Location;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLine;
import com.artipie.scheduling.ArtifactEvent;

import java.security.Permission;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class PushManifestSlice extends DockerActionSlice {

    private final Queue<ArtifactEvent> queue;

    public PushManifestSlice(Docker docker, Queue<ArtifactEvent> queue) {
        super(docker);
        this.queue = queue;
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        ManifestRequest request = ManifestRequest.from(line);
        final ManifestReference ref = request.reference();
        return this.docker.repo(request.name())
            .manifests()
            .put(ref, new Content.From(body))
            .thenApply(
                manifest -> {
                    if (queue != null && ImageTag.valid(ref.digest())) {
                        queue.add(
                            new ArtifactEvent(
                                "docker",
                                docker.registryName(),
                                new Login(headers).getValue(),
                                request.name(), ref.digest(),
                                manifest.layers().stream().mapToLong(ManifestLayer::size).sum()
                            )
                        );
                    }
                    return ResponseBuilder.created()
                        .header(new Location(String.format("/v2/%s/manifests/%s", request.name(), ref.digest())))
                        .header(new ContentLength("0"))
                        .header(new DigestHeader(manifest.digest()))
                        .build();
                }
            );
    }

    @Override
    public Permission permission(RequestLine line) {
        return new DockerRepositoryPermission(
            docker.registryName(), ManifestRequest.from(line).name(), DockerActions.PUSH.mask()
        );
    }
}
