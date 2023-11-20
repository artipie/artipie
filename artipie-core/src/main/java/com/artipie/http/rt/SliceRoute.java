/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.reactivestreams.Publisher;

/**
 * Routing slice.
 * <p>
 * {@link Slice} implementation which redirect requests to {@link Slice}
 * in {@link Path} if {@link RtRule} matched.
 * </p>
 * <p>
 * Usage:
 * </p>
 * <pre><code>
 * new SliceRoute(
 *   new SliceRoute.Path(
 *     new RtRule.ByMethod("GET"), new DownloadSlice(storage)
 *   ),
 *   new SliceRoute.Path(
 *     new RtRule.ByMethod("PUT"), new UploadSlice(storage)
 *   )
 * );
 * </code></pre>
 * @since 0.5
 */
public final class SliceRoute implements Slice {

    /**
     * Routes.
     */
    private final List<RtPath> routes;

    /**
     * New slice route.
     * @param routes Routes
     */
    public SliceRoute(final RtPath... routes) {
        this(Arrays.asList(routes));
    }

    /**
     * New slice route.
     * @param routes Routes
     */
    public SliceRoute(final List<RtPath> routes) {
        this.routes = routes;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return this.routes.stream()
            .map(item -> item.response(line, headers, body))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElse(
                new RsWithBody(
                    new RsWithStatus(RsStatus.NOT_FOUND),
                    "not found", StandardCharsets.UTF_8
                )
            );
    }

    /**
     * Route path.
     * <p>
     * A path to slice with routing rule. If
     * {@link RtRule} passed, then the request will be redirected to
     * underlying {@link Slice}.
     * </p>
     * @since 0.5
     * @deprecated Use {@link RtRulePath} instead
     */
    @Deprecated
    public static final class Path implements RtPath {

        /**
         * Wrapped.
         */
        private final RtPath wrapped;

        /**
         * New routing path.
         * @param rule Rules to apply
         * @param slice Slice to call
         */
        public Path(final RtRule rule, final Slice slice) {
            this.wrapped = new RtRulePath(rule, slice);
        }

        @Override
        public Optional<Response> response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            return this.wrapped.response(line, headers, body);
        }
    }
}
