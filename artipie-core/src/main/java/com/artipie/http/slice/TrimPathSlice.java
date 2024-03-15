/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.ArtipieException;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.google.common.collect.Iterables;
import org.apache.http.client.utils.URIBuilder;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slice that removes the first part from the request URI.
 * <p>
 * For example {@code GET http://www.w3.org/pub/WWW/TheProject.html HTTP/1.1}
 * would be {@code GET http://www.w3.org/WWW/TheProject.html HTTP/1.1}.
 * </p>
 * <p>
 * The full path will be available as the value of {@code X-FullPath} header.
 * </p>
 */
public final class TrimPathSlice implements Slice {

    /**
     * Full path header name.
     */
    private static final String HDR_FULL_PATH = "X-FullPath";

    /**
     * Delegate slice.
     */
    private final Slice slice;

    /**
     * Pattern to trim.
     */
    private final Pattern ptn;

    /**
     * Trim URI path by first hit of path param.
     * @param slice Origin slice
     * @param path Path to trim
     */
    public TrimPathSlice(final Slice slice, final String path) {
        this(
            slice,
            Pattern.compile(String.format("^/(?:%s)(\\/.*)?", TrimPathSlice.normalized(path)))
        );
    }

    /**
     * Trim URI path by pattern.
     *
     * @param slice Origin slice
     * @param ptn Path to trim
     */
    public TrimPathSlice(final Slice slice, final Pattern ptn) {
        this.slice = slice;
        this.ptn = ptn;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final URI uri = line.uri();
        final String full = uri.getPath();
        final Matcher matcher = this.ptn.matcher(full);
        final Response response;
        final boolean recursion = !new RqHeaders(headers, TrimPathSlice.HDR_FULL_PATH).isEmpty();
        if (matcher.matches() && recursion) {
            response = this.slice.response(line, headers, body);
        } else if (matcher.matches() && !recursion) {
            URI respUri;
            try {
                respUri = new URIBuilder(uri)
                    .setPath(asPath(matcher.group(1)))
                    .build();
            } catch (URISyntaxException e) {
                throw new ArtipieException(e);
            }
            response = this.slice.response(
                new RequestLine(line.method(), respUri, line.version()),
                Iterables.concat(
                    headers,
                    Collections.singletonList(new Header(TrimPathSlice.HDR_FULL_PATH, full))
                ),
                body
            );
        } else {
            response = new RsWithStatus(
                new RsWithBody(
                    String.format(
                        "Request path %s was not matched to %s", full, this.ptn
                    ),
                    StandardCharsets.UTF_8
                ),
                RsStatus.INTERNAL_ERROR
            );
        }
        return response;
    }

    /**
     * Normalize path: remove whitespaces and slash chars.
     * @param path Path
     * @return Normalized path
     */
    private static String normalized(final String path) {
        final String clear = Objects.requireNonNull(path).trim();
        if (clear.isEmpty()) {
            return "";
        }
        if (clear.charAt(0) == '/') {
            return normalized(clear.substring(1));
        }
        if (clear.charAt(clear.length() - 1) == '/') {
            return normalized(clear.substring(0, clear.length() - 1));
        }
        return clear;
    }

    /**
     * Convert matched string to valid path.
     * @param result Result of matching
     * @return Path string
     */
    private static String asPath(final String result) {
        if (result == null || result.isEmpty()) {
            return "/";
        }
        if (result.charAt(0) != '/') {
            return '/' + result;
        }
        return result;
    }
}
