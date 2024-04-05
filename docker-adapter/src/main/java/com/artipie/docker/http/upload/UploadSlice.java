/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http.upload;

import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.http.DigestHeader;
import com.artipie.docker.http.DockerActionSlice;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.headers.Location;

import java.util.concurrent.CompletableFuture;

public abstract class UploadSlice extends DockerActionSlice {


    public UploadSlice(Docker docker) {
        super(docker);
    }

    protected CompletableFuture<Response> acceptedResponse(String name, String uuid, long offset) {
        return ResponseBuilder.accepted()
            .header(new Location(String.format("/v2/%s/blobs/uploads/%s", name, uuid)))
            .header(new Header("Range", String.format("0-%d", offset)))
            .header(new ContentLength("0"))
            .header(new Header("Docker-Upload-UUID", uuid))
            .completedFuture();
    }

    protected CompletableFuture<Response> createdResponse(String name, Digest digest) {
        return ResponseBuilder.created()
            .header(new Location(String.format("/v2/%s/blobs/%s", name, digest.string())))
            .header(new ContentLength("0"))
            .header(new DigestHeader(digest))
            .completedFuture();
    }
}
