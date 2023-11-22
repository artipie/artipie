/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.composer.Name;
import com.artipie.composer.Packages;
import com.artipie.composer.Repository;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Slice that serves package metadata.
 *
 * @since 0.3
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

    /**
     * Repository.
     */
    private final Repository repository;

    /**
     * Ctor.
     *
     * @param repository Repository.
     */
    PackageMetadataSlice(final Repository repository) {
        this.repository = repository;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            this.packages(new RequestLineFrom(line).uri().getPath())
                .thenApply(
                    opt -> opt.<Response>map(
                        packages -> new AsyncResponse(packages.content()
                            .thenApply(RsWithBody::new)
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
        final CompletionStage<Optional<Packages>> result;
        final Matcher matcher = PACKAGE.matcher(path);
        if (matcher.find()) {
            result = this.repository.packages(
                new Name(
                    String.format("%s/%s", matcher.group("vendor"), matcher.group("package"))
                )
            );
        } else if (ALL_PACKAGES.matcher(path).matches()) {
            result = this.repository.packages();
        } else {
            throw new IllegalStateException(String.format("Unexpected path: %s", path));
        }
        return result;
    }
}
