/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.Content;
import com.artipie.composer.Repository;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Slice for adding a package to the repository in JSON format.
 *
 * @since 0.3
 */
final class AddSlice implements Slice {

    /**
     * RegEx pattern for matching path.
     */
    public static final Pattern PATH_PATTERN = Pattern.compile("^/(\\?version=(?<version>.*))?$");

    /**
     * Repository.
     */
    private final Repository repository;

    /**
     * Ctor.
     *
     * @param repository Repository.
     */
    AddSlice(final Repository repository) {
        this.repository = repository;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final String path = new RequestLineFrom(line).uri().toString();
        final Matcher matcher = AddSlice.PATH_PATTERN.matcher(path);
        final Response resp;
        if (matcher.matches()) {
            resp = new AsyncResponse(
                this.repository.addJson(
                    new Content.From(body), Optional.ofNullable(matcher.group("version"))
                ).thenApply(nothing -> new RsWithStatus(RsStatus.CREATED))
            );
        } else {
            resp = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return resp;
    }
}
