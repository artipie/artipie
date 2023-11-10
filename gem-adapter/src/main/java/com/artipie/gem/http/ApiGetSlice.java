/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem.http;

import com.artipie.asto.Storage;
import com.artipie.gem.Gem;
import com.artipie.http.ArtipieHttpException;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Returns some basic information about the given gem.
 * <p>
 * Handle {@code GET - /api/v1/gems/[GEM NAME].(json|yaml)}
 * requests, see
 * <a href="https://guides.rubygems.org/rubygems-org-api">RubyGems API</a>
 * for documentation.
 * </p>
 *
 * @since 0.2
 */
final class ApiGetSlice implements Slice {

    /**
     * Endpoint path pattern.
     */
    public static final Pattern PATH_PATTERN = Pattern
        .compile("/api/v1/gems/(?<name>[\\w\\d-]+).(?<fmt>json|yaml)");

    /**
     * Gem SDK.
     */
    private final Gem sdk;

    /**
     * New slice for handling Get API requests.
     * @param storage Gems storage
     */
    ApiGetSlice(final Storage storage) {
        this.sdk = new Gem(storage);
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = PATH_PATTERN.matcher(new RequestLineFrom(line).uri().toString());
        if (!matcher.find()) {
            throw new ArtipieHttpException(
                RsStatus.BAD_REQUEST, String.format("Invalid URI: `%s`", matcher.toString())
            );
        }
        return new AsyncResponse(
            this.sdk.info(matcher.group("name"))
                .thenApply(MetaResponseFormat.byName(matcher.group("fmt")))
        );
    }
}
