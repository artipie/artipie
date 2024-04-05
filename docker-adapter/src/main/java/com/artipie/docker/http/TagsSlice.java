/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Docker;
import com.artipie.docker.misc.ImageRepositoryName;
import com.artipie.docker.misc.Pagination;
import com.artipie.docker.misc.RqByRegex;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLine;

import java.util.concurrent.CompletableFuture;

/**
 * Tags entity in Docker HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/#tags">Tags</a>.
 */
final class TagsSlice extends DockerActionSlice {

    public TagsSlice(Docker docker) {
        super(docker);
    }

    @Override
    public DockerRepositoryPermission permission(RequestLine line) {
        return new DockerRepositoryPermission(
            docker.registry(), name(line), DockerActions.PULL.mask()
        );
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        return this.docker.repo(name(line))
            .manifests()
            .tags(Pagination.from(line.uri()))
            .thenApply(
                tags -> ResponseBuilder.ok()
                    .header(ContentType.json())
                    .body(tags.json())
                    .build()
            );
    }

    private String name(RequestLine line) {
        return ImageRepositoryName.validate(new RqByRegex(line, PathPatterns.TAGS)
            .path().group("name"));
    }
}
