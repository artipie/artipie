/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http.manifest;

import com.artipie.docker.ManifestReference;
import com.artipie.docker.http.PathPatterns;
import com.artipie.docker.misc.ImageRepositoryName;
import com.artipie.docker.misc.RqByRegex;
import com.artipie.http.rq.RequestLine;

public record ManifestRequest(String name, ManifestReference reference) {

    public static ManifestRequest from(RequestLine line) {
        RqByRegex regex = new RqByRegex(line, PathPatterns.MANIFESTS);
        return new ManifestRequest(
            ImageRepositoryName.validate(regex.path().group("name")),
            ManifestReference.from(regex.path().group("reference"))
        );
    }
}