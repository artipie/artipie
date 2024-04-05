/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http.upload;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rq.RequestLine;

import java.security.Permission;
import java.util.concurrent.CompletableFuture;

public class PostUploadSlice extends UploadSlice {

    public PostUploadSlice(Docker docker) {
        super(docker);
    }

    @Override
    public Permission permission(RequestLine line) {
        return new DockerRepositoryPermission(
            docker.registryName(), UploadRequest.from(line).name(), DockerActions.PUSH.mask()
        );
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        UploadRequest request = UploadRequest.from(line);
        if (request.mount().isPresent() && request.from().isPresent()) {
            return mount(request.mount().get(), request.from().get(), request.name());
        }
        return startUpload(request.name());
    }

    /**
     * Mounts specified blob from source repository to target repository.
     *
     * @param digest Blob digest.
     * @param source Source repository name.
     * @param target Target repository name.
     * @return HTTP response.
     */
    private CompletableFuture<Response> mount(
        Digest digest, String source, String target
    ) {
        return this.docker.repo(source)
            .layers()
            .get(digest)
            .thenCompose(
                opt -> opt.map(
                    src -> this.docker.repo(target)
                        .layers()
                        .mount(src)
                        .thenCompose(blob -> createdResponse(target, digest))
                ).orElseGet(
                    () -> this.startUpload(target)
                )
            );
    }

    /**
     * Starts new upload in specified repository.
     *
     * @param name Repository name.
     * @return HTTP response.
     */
    private CompletableFuture<Response> startUpload(String name) {
        return this.docker.repo(name)
            .uploads()
            .start()
            .thenCompose(upload -> acceptedResponse(name, upload.uuid(), 0));
    }
}
