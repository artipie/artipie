/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.ArtipieException;
import com.artipie.docker.RepoName;
import com.artipie.http.rq.RqParams;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Pagination parameters.
 *
 * @param last  last
 * @param limit
 */
public record Pagination(RepoName last, int limit) {

    public static Pagination empty() {
        return from(null, null);
    }

    public static Pagination from(URI uri) {
        final RqParams params = new RqParams(uri);
        return new Pagination(
            params.value("last").map(RepoName.Simple::new).orElse(null),
            params.value("n").map(Integer::parseInt).orElse(Integer.MAX_VALUE)
        );
    }

    public static Pagination from(String repoName, Integer limit) {
        return new Pagination(
            repoName != null ? new RepoName.Simple(repoName) : null,
            limit != null ? limit : Integer.MAX_VALUE
        );
    }

    /**
     * Creates a URI string with pagination parameters.
     *
     * @param uriString a valid URI in string form.
     * @return URI string with pagination parameters.
     */
    public String uriWithPagination(String uriString) {
        try {
            URIBuilder builder = new URIBuilder(uriString);
            if (limit != Integer.MAX_VALUE) {
                builder.addParameter("n", String.valueOf(limit));
            }
            if (last != null) {
                builder.addParameter("last", last.value());
            }
            return builder.toString();
        } catch (URISyntaxException e) {
            throw new ArtipieException(e);
        }
    }

    /**
     * Compares {@code name} and {@code Pagination.last} values.
     * If {@code Pagination.last} than returns {@code true}, else it
     * compares {@code name} and {@code Pagination.last} values.
     * If {@code name} value more than {@code Pagination.last} value returns {@code true}.
     *
     * @param name Image repository name.
     * @return True if given {@code name} more than {@code Pagination.last}.
     */
    public boolean lessThan(RepoName name) {
        return last == null || name.value().compareTo(last.value()) > 0;
    }
}
