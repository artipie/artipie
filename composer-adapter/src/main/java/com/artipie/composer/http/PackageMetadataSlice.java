/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.Content;
import com.artipie.composer.Name;
import com.artipie.composer.Packages;
import com.artipie.composer.Repository;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.rs.StandardRs;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slice that serves package metadata.
 */
public final class PackageMetadataSlice implements Slice {

    /**
     * RegEx pattern for package metadata path.
     * According to <a href="https://packagist.org/apidoc#get-package-data">docs</a>.
     */
    public static final Pattern PACKAGE = Pattern.compile(
        "/p2?/(?<vendor>[^/]+)/(?<package>[^/]+)\\.json$"
    );

    /**
     * RegEx pattern for all packages metadata path.
     */
    public static final Pattern ALL_PACKAGES = Pattern.compile("^/packages.json$");

    private final Repository repository;

    /**
     * @param repository Repository.
     */
    PackageMetadataSlice(final Repository repository) {
        this.repository = repository;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        return new AsyncResponse(
            this.packages(line.uri().getPath())
                .thenApply(
                    opt -> opt.<Response>map(
                        packages -> new AsyncResponse(
                            packages.content()
                                .thenApply(cnt -> BaseResponse.ok().body(cnt))
                        )
                    ).orElse(StandardRs.NOT_FOUND)
                )
        );
    }

    /**
     * Builds key to storage value from path.
     *
     * @param path Resource path.
     * @return Key to storage value.
     */
    private CompletionStage<Optional<Packages>> packages(final String path) {
        final Matcher matcher = PACKAGE.matcher(path);
        if (matcher.find()) {
            return this.repository.packages(
                new Name(matcher.group("vendor") +'/' + matcher.group("package"))
            );
        }
        if (ALL_PACKAGES.matcher(path).matches()) {
            return this.repository.packages();
        }
        throw new IllegalStateException("Unexpected path: "+path);
    }
}
