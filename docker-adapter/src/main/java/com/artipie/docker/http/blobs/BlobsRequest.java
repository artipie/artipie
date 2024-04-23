/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http.blobs;

import com.artipie.docker.Digest;
import com.artipie.docker.http.PathPatterns;
import com.artipie.docker.misc.ImageRepositoryName;
import com.artipie.docker.misc.RqByRegex;
import com.artipie.http.rq.RequestLine;

public record BlobsRequest(String name, Digest digest) {

    public static BlobsRequest from(RequestLine line) {
        RqByRegex regex = new RqByRegex(line, PathPatterns.BLOBS);
        return new BlobsRequest(
            ImageRepositoryName.validate(regex.path().group("name")),
            new Digest.FromString(regex.path().group("digest"))
        );
    }

}
