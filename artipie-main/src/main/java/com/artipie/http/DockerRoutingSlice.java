/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.docker.http.BaseEntity;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.perms.FreePermissions;
import com.artipie.settings.Settings;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.utils.URIBuilder;
import org.reactivestreams.Publisher;

/**
 * Slice decorator which redirects all Docker V2 API requests to Artipie format paths.
 * @since 0.9
 */
public final class DockerRoutingSlice implements Slice {

    /**
     * Real path header name.
     */
    private static final String HDR_REAL_PATH = "X-RealPath";

    /**
     * Docker V2 API path pattern.
     */
    private static final Pattern PTN_PATH = Pattern.compile("/v2((/.*)?)");

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Settings.
     */
    private final Settings settings;

    /**
     * Decorates slice with Docker V2 API routing.
     * @param settings Settings.
     * @param origin Origin slice
     */
    DockerRoutingSlice(final Settings settings, final Slice origin) {
        this.settings = settings;
        this.origin = origin;
    }

    @Override
    @SuppressWarnings("PMD.NestedIfDepthCheck")
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final RequestLineFrom req = new RequestLineFrom(line);
        final String path = req.uri().getPath();
        final Matcher matcher = PTN_PATH.matcher(path);
        final Response rsp;
        if (matcher.matches()) {
            final String group = matcher.group(1);
            if (group.isEmpty() || group.equals("/")) {
                rsp = new BasicAuthzSlice(
                    new BaseEntity(),
                    this.settings.authz().authentication(),
                    new OperationControl(
                        user -> user.isAnonymous() ? EmptyPermissions.INSTANCE
                            : FreePermissions.INSTANCE,
                        new DockerRepositoryPermission("*", "*", DockerActions.PULL.mask())
                    )
                ).response(line, headers, body);
            } else {
                rsp = this.origin.response(
                    new RequestLine(
                        req.method().toString(),
                        new URIBuilder(req.uri()).setPath(group).toString(),
                        req.version()
                    ).toString(),
                    new Headers.From(headers, DockerRoutingSlice.HDR_REAL_PATH, path),
                    body
                );
            }
        } else {
            rsp = this.origin.response(line, headers, body);
        }
        return rsp;
    }

    /**
     * Slice which reverts real path from headers if exists.
     * @since 0.9
     */
    public static final class Reverted implements Slice {

        /**
         * Origin slice.
         */
        private final Slice origin;

        /**
         * New {@link Slice} decorator to revert real path.
         * @param origin Origin slice
         */
        public Reverted(final Slice origin) {
            this.origin = origin;
        }

        @Override
        public Response response(final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body) {
            final RequestLineFrom req = new RequestLineFrom(line);
            return this.origin.response(
                new RequestLine(
                    req.method().toString(),
                    new URIBuilder(req.uri())
                        .setPath(String.format("/v2%s", req.uri().getPath()))
                        .toString(),
                    req.version()
                ).toString(),
                headers,
                body
            );
        }
    }
}
