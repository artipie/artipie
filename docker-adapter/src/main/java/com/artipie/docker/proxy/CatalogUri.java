/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.RepoName;
import com.google.common.base.Joiner;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * URI for catalog resource.
 *
 * @since 0.10
 */
final class CatalogUri {

    /**
     * From which repository to start, exclusive.
     */
    private final Optional<RepoName> from;

    /**
     * Maximum number of repositories returned.
     */
    private final int limit;

    /**
     * Ctor.
     *
     * @param from From which repository to start, exclusive.
     * @param limit Maximum number of repositories returned.
     */
    CatalogUri(final Optional<RepoName> from, final int limit) {
        this.from = from;
        this.limit = limit;
    }

    /**
     * Build URI string.
     *
     * @return URI string.
     */
    public String string() {
        final Stream<String> nparam;
        if (this.limit < Integer.MAX_VALUE) {
            nparam = Stream.of(String.format("n=%d", this.limit));
        } else {
            nparam = Stream.empty();
        }
        final List<String> params = Stream.concat(
            nparam,
            this.from.map(name -> Stream.of(String.format("last=%s", name.value())))
                .orElseGet(Stream::empty)
        ).collect(Collectors.toList());
        final StringBuilder uri = new StringBuilder("/v2/_catalog");
        if (!params.isEmpty()) {
            uri.append(String.format("?%s", Joiner.on("&").join(params)));
        }
        return uri.toString();
    }
}
